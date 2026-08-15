package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.*;
import com.kinloop.backend.entity.enums.*;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ActivityEligibilityPolicy {
    public boolean allows(Activity a, ChildProfileSnapshot profile, Map<String, BigDecimal> p) {
        if (profile.getFocusBand() == FocusBand.SHORT && a.getDurationMinutes() > p.get("short_focus_max_duration_minutes").intValue())
            return false;
        if (profile.getSeparationAnxiety() != null && profile.getSeparationAnxiety() >= p.get("high_separation_anxiety_threshold").intValue() && a.getInvolvementType() == InvolvementType.BAGIMSIZ)
            return false;
        if (profile.getDunnQuadrant() == DunnQuadrant.C4)
            return Math.max(a.getNoiseLoad(), Math.max(a.getVisualLoad(), a.getPhysicalIntensity())) < p.get("c4_hard_filter_load_threshold").intValue();
        return true;
    }
}
