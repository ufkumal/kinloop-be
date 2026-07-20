package com.kinloop.backend.entity.enums;

import java.time.LocalDate;
import java.time.Period;

public enum AgeBand {
    BAND_0_12(0, 12),
    BAND_12_24(12, 24),
    BAND_24_48(24, 48),
    BAND_48_72(48, 72);

    public static final int MAX_SUPPORTED_MONTHS = 72;

    private final int minMonths;
    private final int maxMonths;

    AgeBand(int minMonths, int maxMonths) {
        this.minMonths = minMonths;
        this.maxMonths = maxMonths;
    }

    public static AgeBand resolve(int ageMonths) {
        for (AgeBand band : values()) {
            if (band.covers(ageMonths)) {
                return band;
            }
        }
        throw new IllegalArgumentException("Unsupported age in months: " + ageMonths);
    }

    public boolean covers(int ageMonths) {
        if (this == BAND_48_72) {
            return ageMonths >= minMonths && ageMonths <= maxMonths;
        }
        return ageMonths >= minMonths && ageMonths < maxMonths;
    }

    public static boolean isSupported(int ageMonths) {
        return ageMonths >= 0 && ageMonths <= MAX_SUPPORTED_MONTHS;
    }

    public static int ageInMonths(LocalDate birthDate, LocalDate onDate) {
        Period period = Period.between(birthDate, onDate);
        return period.getYears() * 12 + period.getMonths();
    }
}
