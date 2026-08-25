package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.kinloop.backend.entity.ConsentDocument;
import com.kinloop.backend.entity.UserConsent;
import com.kinloop.backend.entity.enums.ConsentType;
import com.kinloop.backend.repository.ConsentDocumentRepository;
import com.kinloop.backend.repository.UserConsentRepository;
import java.time.OffsetDateTime;
import java.util.List;
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
    void allActiveRequiredDocumentVersionsMustBeGranted() {
        ConsentDocument terms = document(1L, true);
        ConsentDocument privacy = document(2L, true);
        ConsentDocument marketing = document(3L, false);
        when(consentDocumentRepository.findByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(terms, privacy, marketing));
        when(userConsentRepository.findByUserIdAndConsentDocumentIdIn(8L, List.of(1L, 2L)))
                .thenReturn(List.of(granted(terms)));

        assertFalse(service.hasGrantedAllRequiredConsents(8L));
    }

    @Test
    void optionalConsentDoesNotBlockActivityGeneration() {
        ConsentDocument terms = document(1L, true);
        ConsentDocument marketing = document(3L, false);
        when(consentDocumentRepository.findByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(terms, marketing));
        when(userConsentRepository.findByUserIdAndConsentDocumentIdIn(8L, List.of(1L)))
                .thenReturn(List.of(granted(terms)));

        assertTrue(service.hasGrantedAllRequiredConsents(8L));
    }

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

    private ConsentDocument document(Long id, boolean required) {
        ConsentDocument document = new ConsentDocument();
        document.setId(id);
        document.setRequired(required);
        return document;
    }

    private UserConsent granted(ConsentDocument document) {
        UserConsent consent = new UserConsent();
        consent.setConsentDocument(document);
        consent.setGranted(true);
        consent.setRespondedAt(OffsetDateTime.now());
        consent.setUpdatedAt(OffsetDateTime.now());
        return consent;
    }
}
