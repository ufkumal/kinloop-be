package com.kinloop.backend;

import com.kinloop.backend.repository.*;
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
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "jwt.secret=kinloop-test-secret-key-at-least-32-bytes"
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
        QuestionOptionRepository questionOptionRepository() { return repositoryProxy(QuestionOptionRepository.class); }

        @Bean
        ChildAnswerRepository childAnswerRepository() { return repositoryProxy(ChildAnswerRepository.class); }

        @Bean
        ChildProfileSnapshotRepository childProfileSnapshotRepository() { return repositoryProxy(ChildProfileSnapshotRepository.class); }

        @Bean
        DailyPlanItemRepository dailyPlanItemRepository() { return repositoryProxy(DailyPlanItemRepository.class); }

        @Bean
        DailyPlanRepository dailyPlanRepository() { return repositoryProxy(DailyPlanRepository.class); }

        @Bean
        WorkshopProfileRepository workshopProfileRepository() {
            return repositoryProxy(WorkshopProfileRepository.class);
        }

        @Bean
        ConsentDocumentRepository consentDocumentRepository() { return repositoryProxy(ConsentDocumentRepository.class); }

        @Bean
        UserConsentRepository userConsentRepository() { return repositoryProxy(UserConsentRepository.class); }

        @Bean
        FeedbackRepository feedbackRepository() { return repositoryProxy(FeedbackRepository.class); }

        @Bean
        FeedbackEffectRepository feedbackEffectRepository() { return repositoryProxy(FeedbackEffectRepository.class); }

        @Bean
        FeedbackLlmClassificationRepository feedbackLlmClassificationRepository() { return repositoryProxy(FeedbackLlmClassificationRepository.class); }

        @Bean
        DunnProfileRepository dunnProfileRepository() { return repositoryProxy(DunnProfileRepository.class); }

        @Bean
        ChildIntelligenceScoreRepository childIntelligenceScoreRepository() { return repositoryProxy(ChildIntelligenceScoreRepository.class); }

        @Bean
        ChildDomainLevelRepository childDomainLevelRepository() { return repositoryProxy(ChildDomainLevelRepository.class); }

        @Bean
        ChildSensoryAdjustmentRepository childSensoryAdjustmentRepository() { return repositoryProxy(ChildSensoryAdjustmentRepository.class); }

        @Bean
        ScoringParameterRepository scoringParameterRepository() { return repositoryProxy(ScoringParameterRepository.class); }

        @Bean
        ActivityRepository activityRepository() { return repositoryProxy(ActivityRepository.class); }

        @Bean
        DevelopmentalPeriodTaskRepository developmentalPeriodTaskRepository() { return repositoryProxy(DevelopmentalPeriodTaskRepository.class); }

        @Bean
        RecommendationRepository recommendationRepository() { return repositoryProxy(RecommendationRepository.class); }

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
