package com.kinloop.backend.service.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SecondhandReportDetectorTest {
    private final SecondhandReportDetector detector = new SecondhandReportDetector();

    @Test
    void detectsV2SecondhandExample() {
        assertTrue(detector.isSecondhand("Babası yaptırmış, çok beğenmiş diyor, ben görmedim"));
        assertTrue(detector.isSecondhand("Öğretmeni çok sevdiğini söyledi"));
    }

    @Test
    void doesNotCapDirectObservation() {
        assertFalse(detector.isSecondhand("Yanında oturdum, şarkıyı söylerken gördüm"));
    }
}
