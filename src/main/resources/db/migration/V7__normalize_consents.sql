-- Consent documents are immutable, versioned records displayed by the frontend.
CREATE TABLE consent_documents (
    id            BIGSERIAL PRIMARY KEY,
    consent_type  VARCHAR(40)  NOT NULL
        CHECK (consent_type IN ('TERMS', 'PRIVACY', 'KVKK', 'MARKETING', 'DATA_PROCESSING')),
    version       VARCHAR(20)  NOT NULL,
    title         VARCHAR(200) NOT NULL,
    summary       TEXT,
    content       TEXT         NOT NULL,
    required      BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INTEGER      NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT FALSE,
    effective_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_consent_documents_type_version UNIQUE (consent_type, version)
);

CREATE UNIQUE INDEX uq_consent_documents_active_type
    ON consent_documents (consent_type) WHERE is_active;
CREATE INDEX idx_consent_documents_active_order
    ON consent_documents (is_active, display_order);

-- A decision points to the exact document version the user saw.
CREATE TABLE user_consents (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT      NOT NULL
        CONSTRAINT fk_user_consents_user REFERENCES users (id) ON DELETE CASCADE,
    consent_document_id BIGINT      NOT NULL
        CONSTRAINT fk_user_consents_document REFERENCES consent_documents (id),
    granted             BOOLEAN     NOT NULL,
    responded_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at          TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_user_consents_user_document UNIQUE (user_id, consent_document_id),
    CONSTRAINT ck_user_consents_revocation CHECK (granted OR revoked_at IS NOT NULL)
);

CREATE INDEX idx_user_consents_user ON user_consents (user_id);

-- Preserve decisions from the original mixed table. Its nullable version is
-- normalized because every decision must now identify an exact document.
INSERT INTO consent_documents (consent_type, version, title, content, is_active, effective_at)
SELECT DISTINCT consent_type,
       COALESCE(NULLIF(version, ''), 'legacy'),
       initcap(replace(consent_type, '_', ' ')),
       'Legacy consent text was not stored by the previous schema.',
       FALSE,
       MIN(granted_at)
FROM consents
GROUP BY consent_type, COALESCE(NULLIF(version, ''), 'legacy');

INSERT INTO user_consents
    (user_id, consent_document_id, granted, responded_at, revoked_at, updated_at)
SELECT DISTINCT ON (c.user_id, d.id)
       c.user_id, d.id, (c.granted AND c.revoked_at IS NULL), c.granted_at,
       CASE WHEN c.granted AND c.revoked_at IS NULL THEN NULL ELSE COALESCE(c.revoked_at, c.granted_at) END,
       COALESCE(c.revoked_at, c.granted_at)
FROM consents c
JOIN consent_documents d
  ON d.consent_type = c.consent_type
 AND d.version = COALESCE(NULLIF(c.version, ''), 'legacy')
ORDER BY c.user_id, d.id, COALESCE(c.revoked_at, c.granted_at) DESC;

DROP TABLE consents;

-- Dummy v1 documents. Replace the content after legal review; never edit a
-- published version in place--insert v2 and activate it instead.
INSERT INTO consent_documents
    (consent_type, version, title, summary, content, required, display_order, is_active, effective_at)
VALUES
    ('TERMS', '1.0', 'Terms of Use',
     'Rules for using Kinloop.',
     E'# Terms of Use\n\nBy using Kinloop, you agree to use the service responsibly and provide accurate account information. This is placeholder content and must be reviewed before production use.',
     TRUE, 10, TRUE, now()),
    ('PRIVACY', '1.0', 'Privacy Notice',
     'How Kinloop handles account and child profile data.',
     E'# Privacy Notice\n\nKinloop uses the information you provide to operate and improve the service. This is placeholder content and must be reviewed before production use.',
     TRUE, 20, TRUE, now()),
    ('KVKK', '1.0', 'KVKK Aydinlatma Metni',
     'Kisisel verilerin islenmesine iliskin bilgilendirme.',
     E'# KVKK Aydinlatma Metni\n\nKisisel verileriniz Kinloop hizmetinin sunulmasi amaciyla islenir. Bu taslak metin yayina alinmadan once hukuk incelemesinden gecmelidir.',
     TRUE, 30, TRUE, now()),
    ('MARKETING', '1.0', 'Marketing Communications',
     'Optional product news and offers.',
     E'# Marketing Communications\n\nI agree to receive optional Kinloop product news and offers. I can withdraw this permission at any time.',
     FALSE, 40, TRUE, now()),
    ('DATA_PROCESSING', '1.0', 'Data Processing Consent',
     'Permission for optional personalization processing.',
     E'# Data Processing Consent\n\nI agree to the optional processing described here for personalized Kinloop experiences. This is placeholder content and must be reviewed before production use.',
     FALSE, 50, TRUE, now())
ON CONFLICT (consent_type, version) DO UPDATE SET
    title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    content = EXCLUDED.content,
    required = EXCLUDED.required,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active,
    effective_at = EXCLUDED.effective_at;
