package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.Activity;

import java.math.BigDecimal;
import java.util.Map;

public record ScoredActivity(Activity activity, BigDecimal rawScore, BigDecimal displayScore,
                             Map<String, Object> breakdown) {
}
