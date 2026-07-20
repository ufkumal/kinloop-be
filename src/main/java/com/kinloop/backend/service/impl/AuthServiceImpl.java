package com.kinloop.backend.service.impl;

import com.kinloop.backend.dto.auth.AuthResponse;
import com.kinloop.backend.dto.auth.LoginRequest;
import com.kinloop.backend.dto.auth.RegisterRequest;
import com.kinloop.backend.dto.auth.RegisterResponse;
import com.kinloop.backend.entity.EmailVerificationToken;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.User;
import com.kinloop.backend.entity.enums.UserRole;
import com.kinloop.backend.exception.AccountNotActiveException;
import com.kinloop.backend.exception.EmailAlreadyExistsException;
import com.kinloop.backend.exception.EmailNotVerifiedException;
import com.kinloop.backend.exception.InvalidCredentialsException;
import com.kinloop.backend.exception.InvalidTokenException;
import com.kinloop.backend.repository.EmailVerificationTokenRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.repository.UserRepository;
import com.kinloop.backend.security.JwtService;
import com.kinloop.backend.service.AuthService;
import com.kinloop.backend.service.EmailService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final long EMAIL_VERIFICATION_EXPIRY_HOURS = 24;

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final ParentProfileRepository parentProfileRepository;

    public AuthServiceImpl(
            UserRepository userRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService, ParentProfileRepository parentProfileRepository
    ) {
        this.userRepository = userRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.parentProfileRepository = parentProfileRepository;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new EmailAlreadyExistsException();
        }
        if (req.role() != UserRole.PARENT && req.role() != UserRole.WORKSHOP) {
            throw new IllegalArgumentException("role must be PARENT or WORKSHOP");
        }

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        user.setEmailVerified(false);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        ParentProfile parentProfile = new ParentProfile();
        parentProfile.setUserId(user.getId());
        parentProfileRepository.save(parentProfile);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(savedUser);
        verificationToken.setExpiresAt(OffsetDateTime.now().plusHours(EMAIL_VERIFICATION_EXPIRY_HOURS));
        verificationToken.setUsedAt(null);
        emailVerificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(savedUser.getEmail(), token);
        return new RegisterResponse("Registration successful. Please verify your email.");
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(InvalidTokenException::new);

        if (verificationToken.isUsed() || verificationToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidTokenException();
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        verificationToken.setUsedAt(OffsetDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req, String userAgent) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }
        if (!user.isActive()) {
            throw new AccountNotActiveException();
        }

        logDeviceMetadata(user, req.deviceLabel(), userAgent);
        String token = jwtService.generateToken(toUserDetails(user));
        return new AuthResponse(token, user.getRole(), user.getId(), user.isEmailVerified());
    }

    private void logDeviceMetadata(User user, String deviceLabel, String userAgent) {
        log.info(
                "Login device metadata for userId={}: deviceLabel={}, userAgent={}",
                user.getId(),
                deviceLabel,
                userAgent
        );
    }

    private UserDetails toUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
