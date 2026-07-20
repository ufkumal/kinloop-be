package com.kinloop.backend;

import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.EmailVerificationTokenRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.repository.UserRepository;
import com.kinloop.backend.repository.WorkshopProfileRepository;
import com.kinloop.backend.repository.QuestionRepository;
import com.kinloop.backend.repository.QuestionnaireSessionRepository;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        }
)
@Import(KinloopBackendApplicationTests.SecurityTestConfig.class)
class KinloopBackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    static class SecurityTestConfig {

        @Bean
        @Primary
        UserDetailsService userDetailsService() {
            return username -> {
                throw new UsernameNotFoundException(username);
            };
        }

        @Bean
        UserRepository userRepository() {
            return repositoryProxy(UserRepository.class);
        }

        @Bean
        EmailVerificationTokenRepository emailVerificationTokenRepository() {
            return repositoryProxy(EmailVerificationTokenRepository.class);
        }

        @Bean
        ChildRepository childRepository() {
            return repositoryProxy(ChildRepository.class);
        }

        @Bean
        ParentProfileRepository parentProfileRepository() {
            return repositoryProxy(ParentProfileRepository.class);
        }

        @Bean
        QuestionRepository questionRepository() {
            return repositoryProxy(QuestionRepository.class);
        }

        @Bean
        QuestionnaireSessionRepository questionnaireSessionRepository() {
            return repositoryProxy(QuestionnaireSessionRepository.class);
        }

        @Bean
        WorkshopProfileRepository workshopProfileRepository() {
            return repositoryProxy(WorkshopProfileRepository.class);
        }

        private static <T> T repositoryProxy(Class<T> repositoryType) {
            Object proxy = Proxy.newProxyInstance(
                    repositoryType.getClassLoader(),
                    new Class<?>[]{repositoryType},
                    (target, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> repositoryType.getSimpleName() + "TestProxy";
                                case "hashCode" -> System.identityHashCode(target);
                                case "equals" -> target == args[0];
                                default -> throw new UnsupportedOperationException(method.getName());
                            };
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }
            );
            return repositoryType.cast(proxy);
        }
    }
}
