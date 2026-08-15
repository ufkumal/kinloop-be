-- Keep child learning-state domains aligned with activities.target_domain.
-- Existing rows remain valid; this only admits the two v5 domains that were
-- added to the activity catalogue in V9.

ALTER TABLE child_domain_levels
    DROP CONSTRAINT child_domain_levels_domain_check;

ALTER TABLE child_domain_levels
    ADD CONSTRAINT chk_child_domain_levels_domain
        CHECK (domain IN (
            'GROSS_MOTOR',
            'FINE_MOTOR',
            'LANGUAGE',
            'SOCIAL_EMOTIONAL',
            'COGNITIVE',
            'SENSORY',
            'SELF_REGULATION'
        ));
