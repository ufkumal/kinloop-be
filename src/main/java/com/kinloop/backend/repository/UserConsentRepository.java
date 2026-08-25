package com.kinloop.backend.repository;

import com.kinloop.backend.entity.UserConsent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {
    List<UserConsent> findByUserIdAndConsentDocumentIdIn(Long userId, List<Long> consentDocumentIds);
    Optional<UserConsent> findByUserIdAndConsentDocumentId(Long userId, Long consentDocumentId);
    boolean existsByUserIdAndConsentDocumentIdAndGrantedTrue(Long userId, Long consentDocumentId);
}
