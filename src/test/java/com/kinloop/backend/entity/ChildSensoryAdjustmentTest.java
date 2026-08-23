package com.kinloop.backend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kinloop.backend.entity.enums.InvolvementFilter;
import com.kinloop.backend.entity.enums.InvolvementHint;
import com.kinloop.backend.entity.enums.SensoryHint;
import org.junit.jupiter.api.Test;

class ChildSensoryAdjustmentTest {

    @Test
    void noiseHintTightensOnlyNoiseAxis() {
        ChildSensoryAdjustment adjustment = new ChildSensoryAdjustment(7L, (short) 0, (short) 0, (short) 0, null);
        adjustment.applySensoryAdjustment(SensoryHint.NOISE, (short) 1);
        assertEquals((short) -1, adjustment.getNoiseAdjustment());
        assertEquals((short) 0, adjustment.getVisualAdjustment());
        assertEquals((short) 0, adjustment.getMovementAdjustment());
    }

    @Test
    void crowdingHintTightensNoiseAndVisualAxes() {
        ChildSensoryAdjustment adjustment = new ChildSensoryAdjustment(7L, (short) 0, (short) 0, (short) 0, null);
        adjustment.applySensoryAdjustment(SensoryHint.CROWDING, (short) 1);
        assertEquals((short) -1, adjustment.getNoiseAdjustment());
        assertEquals((short) -1, adjustment.getVisualAdjustment());
        assertEquals((short) 0, adjustment.getMovementAdjustment());
    }

    @Test
    void repeatedHintsAccumulateUnbounded() {
        ChildSensoryAdjustment adjustment = new ChildSensoryAdjustment(7L, (short) 0, (short) 0, (short) 0, null);
        adjustment.applySensoryAdjustment(SensoryHint.MOVEMENT, (short) 1);
        adjustment.applySensoryAdjustment(SensoryHint.MOVEMENT, (short) 1);
        adjustment.applySensoryAdjustment(SensoryHint.MOVEMENT, (short) 1);
        assertEquals((short) -3, adjustment.getMovementAdjustment());
    }

    @Test
    void togetherHintSetsStrictFilter() {
        ChildSensoryAdjustment adjustment = new ChildSensoryAdjustment(7L, (short) 0, (short) 0, (short) 0, null);
        adjustment.applyInvolvementFilter(InvolvementHint.TOGETHER);
        assertEquals(InvolvementFilter.STRICT, adjustment.getInvolvementFilter());
    }

    @Test
    void aloneHintSetsRelaxedFilter() {
        ChildSensoryAdjustment adjustment = new ChildSensoryAdjustment(
                7L, (short) 0, (short) 0, (short) 0, InvolvementFilter.STRICT);
        adjustment.applyInvolvementFilter(InvolvementHint.ALONE);
        assertEquals(InvolvementFilter.RELAXED, adjustment.getInvolvementFilter());
    }
}
