package com.kinloop.backend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kinloop.backend.entity.enums.DevelopmentDomain;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ChildDomainLevelTest {

    @Test
    void levelUpResetsStreakAndLevelFourCounterIsCapped() {
        ChildDomainLevel level = new ChildDomainLevel(1L, DevelopmentDomain.LANGUAGE, (short) 3);

        apply(level, "1.0");
        apply(level, "1.0");
        apply(level, "1.0");
        apply(level, "0.5");
        apply(level, "0.5");
        apply(level, "0.5");

        assertEquals((short) 4, level.getLevel());
        assertEquals(0, new BigDecimal("1.0").compareTo(level.getStreak()));
    }

    @Test
    void levelDownThresholdIsStrictAndFloorIsEnforced() {
        ChildDomainLevel level = new ChildDomainLevel(1L, DevelopmentDomain.LANGUAGE, (short) 2);

        apply(level, "-1.0");
        assertEquals((short) 2, level.getLevel());
        apply(level, "-1.0");
        assertEquals((short) 1, level.getLevel());
        assertEquals(0, BigDecimal.ZERO.compareTo(level.getStreak()));

        apply(level, "-1.0");
        apply(level, "-1.0");
        assertEquals((short) 1, level.getLevel());
        assertEquals(0, BigDecimal.ZERO.compareTo(level.getStreak()));
    }

    private void apply(ChildDomainLevel level, String delta) {
        level.applyFeedback(new BigDecimal(delta), new BigDecimal("3.0"), new BigDecimal("-1.0"),
                (short) 1, (short) 4, new BigDecimal("1.0"));
    }
}
