package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.kinloop.backend.entity.ConsentDocument;
import com.kinloop.backend.entity.enums.ConsentType;
import com.kinloop.backend.repository.ConsentDocumentRepository;
import com.kinloop.backend.repository.UserConsentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {
    @Mock private ConsentDocumentRepository consentDocumentRepository;
    @Mock private UserConsentRepository userConsentRepository;
    @InjectMocks private ConsentService service;

    @Test
    void falseWhenNoActiveDocumentExistsForType() {
        when(consentDocumentRepository.findFirstByTypeAndActiveTrue(ConsentType.DATA_PROCESSING))
                .thenReturn(Optional.empty());

        assertFalse(service.hasGrantedConsent(5L, ConsentType.DATA_PROCESSING));
    }

    @Test
    void falseWhenUserHasNoGrantedDecision() {
        ConsentDocument document = new ConsentDocument();
        ReflectionTestUtils.setField(document, "id", 9L);
        when(consentDocumentRepository.findFirstByTypeAndActiveTrue(ConsentType.DATA_PROCESSING))
                .thenReturn(Optional.of(document));
        when(userConsentRepository.existsByUserIdAndConsentDocumentIdAndGrantedTrue(5L, 9L))
                .thenReturn(false);

        assertFalse(service.hasGrantedConsent(5L, ConsentType.DATA_PROCESSING));
    }

    @Test
    void trueWhenUserGrantedTheActiveDocument() {
        ConsentDocument document = new ConsentDocument();
        ReflectionTestUtils.setField(document, "id", 9L);
        when(consentDocumentRepository.findFirstByTypeAndActiveTrue(ConsentType.DATA_PROCESSING))
                .thenReturn(Optional.of(document));
        when(userConsentRepository.existsByUserIdAndConsentDocumentIdAndGrantedTrue(5L, 9L))
                .thenReturn(true);

        assertTrue(service.hasGrantedConsent(5L, ConsentType.DATA_PROCESSING));
    }
}
