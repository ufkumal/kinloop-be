package com.kinloop.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_consents")
public class UserConsent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consent_document_id", nullable = false)
    private ConsentDocument consentDocument;

    @Column(nullable = false)
    private boolean granted;

    @Column(name = "responded_at", nullable = false)
    private OffsetDateTime respondedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
