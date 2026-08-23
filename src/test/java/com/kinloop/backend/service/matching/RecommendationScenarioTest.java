package com.kinloop.backend.service.matching;

import static com.kinloop.backend.service.matching.MatchingTestFixtures.levelOneDomains;
import static com.kinloop.backend.service.matching.MatchingTestFixtures.neutralIntelligenceScores;
import static com.kinloop.backend.service.matching.MatchingTestFixtures.parameters;
import static com.kinloop.backend.service.matching.MatchingTestFixtures.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.ActivityInstruction;
import com.kinloop.backend.entity.ChildDomainLevel;
import com.kinloop.backend.entity.ChildIntelligenceScore;
import com.kinloop.backend.entity.ChildProfileSnapshot;
import com.kinloop.backend.entity.DunnProfile;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.DunnQuadrant;
import com.kinloop.backend.entity.enums.FocusBand;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementType;
import com.kinloop.backend.entity.enums.PlanSlotType;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Acceptance-level regression tests derived from the 15 August 2026 examples
 * and aligned with the v6 filtering and scoring rules.
 * The canonical activity migration is treated as test input; repositories and Spring
 * are deliberately not involved, so failures point at matching policy changes.
 */
class RecommendationScenarioTest {
    private static final String ACTIVITY_SEED = "db/migration/V10__kidloop_128_home_activities.sql";
    private static final Pattern ACTIVITY_ROW = Pattern.compile(
            "^\\s*\\(\\d+, 'HOME', NULL,.*?\\bTRUE\\)(?:,|;|\\s*$)",
            Pattern.MULTILINE | Pattern.DOTALL);

    private final ActivityEligibilityPolicy eligibility = new ActivityEligibilityPolicy();
    private final ActivityScorer scorer = new ActivityScorer();
    private final DailyPortfolioBuilder portfolioBuilder = new DailyPortfolioBuilder();
    private final List<Activity> activities = loadActivities();

    @TestFactory
    Stream<DynamicTest> matchesReferenceScenarios() {
        return scenarios().stream().map(scenario -> DynamicTest.dynamicTest(
                scenario.name(), () -> assertScenario(scenario)));
    }

    private void assertScenario(Scenario scenario) {
        ChildProfileSnapshot profile = profile(scenario.quadrant(), scenario.anxiety());
        Map<IntelligenceType, ChildIntelligenceScore> intelligenceScores = neutralIntelligenceScores();
        Map<DevelopmentDomain, ChildDomainLevel> domainLevels = levelOneDomains();
        scenario.configure().apply(intelligenceScores, domainLevels);

        List<Activity> pool = activities.stream()
                .filter(activity -> activity.getMinAgeMonths() <= scenario.ageMonths())
                .filter(activity -> activity.getMaxAgeMonths() >= scenario.ageMonths())
                .filter(activity -> activity.getDurationMinutes() <= scenario.budgetMinutes())
                .filter(activity -> eligibility.allows(activity, profile, null, parameters()))
                .toList();

        DevelopmentDomain period = periodFor(scenario.ageMonths());
        DunnProfile dunn = dunnProfile(scenario.quadrant());
        List<ScoredActivity> preFreshnessScored = pool.stream()
                .map(activity -> scorer.score(activity, profile, dunn, null, period,
                        intelligenceScores, domainLevels, parameters()))
                .sorted(Comparator.comparing(ScoredActivity::rawScore).reversed()
                        .thenComparing(value -> value.activity().getId()))
                .toList();
        List<ScoredActivity> freshScored = preFreshnessScored.stream()
                .filter(candidate -> !scenario.yesterday().contains(candidate.activity().getId()))
                .toList();

        DailyPortfolioBuilder.Result plan = buildPlan(
                freshScored, preFreshnessScored, scenario.yesterday(), period,
                intelligenceScores, scenario.budgetMinutes());

        assertEquals(scenario.expectedPoolSize(), pool.size(), "eligible pool");
        assertEquals(Math.min(3, pool.size()), plan.selections().size(), "fallback output size");
        assertEquals(plan.selections().size(), plan.selections().stream()
                .map(item -> item.activity().activity().getId()).distinct().count(), "distinct activities");
        assertEquals(List.of(PlanSlotType.DEVELOP, PlanSlotType.STRENGTHEN, PlanSlotType.EXPLORE)
                        .subList(0, plan.selections().size()),
                plan.selections().stream().map(DailyPortfolioBuilder.Selection::slot).toList(), "slot order");
        assertTrue(plan.committedDurationMinutes() <= scenario.budgetMinutes(), "committed budget");
        assertEquals(plan.selections().stream()
                        .mapToInt(item -> item.activity().activity().getDurationMinutes()).sum(),
                plan.totalDurationMinutes(), "persisted total duration");
        assertFalse(plan.selections().isEmpty(), "the seeded scenario pool is non-empty");
    }

    private DailyPortfolioBuilder.Result buildPlan(
            List<ScoredActivity> freshScored,
            List<ScoredActivity> preFreshnessScored,
            Set<Long> recentIds,
            DevelopmentDomain period,
            Map<IntelligenceType, ChildIntelligenceScore> intelligenceScores,
            int budget) {
        return portfolioBuilder.build(new DailyPortfolioBuilder.Request(
                freshScored, preFreshnessScored, recentIds,
                period, intelligenceScores, budget, false));
    }

    private ChildProfileSnapshot profile(DunnQuadrant quadrant, int anxiety) {
        ChildProfileSnapshot profile = new ChildProfileSnapshot();
        profile.setDunnQuadrant(quadrant);
        profile.setSeparationAnxiety(anxiety);
        profile.setFocusBand(FocusBand.MEDIUM);
        return profile;
    }

    private DunnProfile dunnProfile(DunnQuadrant quadrant) {
        DunnProfile profile = new DunnProfile();
        set(profile, "quadrant", quadrant);
        switch (quadrant) {
            case C1, MIXED -> tolerances(profile, 3, 3, 3, "5", "5", "3");
            case C2 -> tolerances(profile, 4, 4, 5, "3", "3", "6");
            case C3 -> tolerances(profile, 2, 2, 3, "10", "10", "6");
            case C4 -> tolerances(profile, 1, 2, 2, "5", "5", "3");
        }
        return profile;
    }

    private void tolerances(DunnProfile profile, int noise, int visual, int movement,
                            String noiseWeight, String visualWeight, String movementWeight) {
        set(profile, "noiseTolerance", (short) noise);
        set(profile, "visualTolerance", (short) visual);
        set(profile, "movementTolerance", (short) movement);
        set(profile, "noiseWeight", decimal(noiseWeight));
        set(profile, "visualWeight", decimal(visualWeight));
        set(profile, "movementWeight", decimal(movementWeight));
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private DevelopmentDomain periodFor(int ageMonths) {
        if (ageMonths < 24) return DevelopmentDomain.GROSS_MOTOR;
        if (ageMonths < 48) return DevelopmentDomain.LANGUAGE;
        return DevelopmentDomain.SOCIAL_EMOTIONAL;
    }

    private List<Activity> loadActivities() {
        String sql;
        try (var input = getClass().getClassLoader().getResourceAsStream(ACTIVITY_SEED)) {
            if (input == null) throw new IllegalStateException("Missing activity seed: " + ACTIVITY_SEED);
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read activity seed", exception);
        }

        int activitiesInsert = sql.indexOf("INSERT INTO activities");
        int upsertClause = sql.indexOf("ON CONFLICT (id) DO UPDATE", activitiesInsert);
        Matcher rows = ACTIVITY_ROW.matcher(sql.substring(activitiesInsert, upsertClause));
        List<Activity> result = new ArrayList<>();
        while (rows.find()) result.add(activity(tokens(rows.group())));
        if (result.size() != 128) {
            throw new IllegalStateException("Expected 128 seeded activities but parsed " + result.size());
        }
        return List.copyOf(result);
    }

    private Activity activity(List<String> row) {
        Activity activity = new Activity();
        set(activity, "id", Long.parseLong(row.get(0)));
        set(activity, "scope", "HOME");
        set(activity, "status", "PUBLISHED");
        set(activity, "title", row.get(3));
        set(activity, "description", row.get(4));
        set(activity, "minAgeMonths", Integer.parseInt(row.get(5)));
        set(activity, "maxAgeMonths", Integer.parseInt(row.get(6)));
        set(activity, "targetIntelligence", IntelligenceType.valueOf(row.get(8)));
        set(activity, "secondaryIntelligence", "NULL".equals(row.get(9)) ? null : IntelligenceType.valueOf(row.get(9)));
        set(activity, "targetDomain", DevelopmentDomain.valueOf(row.get(10)));
        set(activity, "difficulty", Short.parseShort(row.get(11)));
        set(activity, "durationMinutes", Short.parseShort(row.get(12)));
        set(activity, "involvementType", InvolvementType.valueOf(row.get(13)));
        set(activity, "noiseLoad", Short.parseShort(row.get(14)));
        set(activity, "visualLoad", Short.parseShort(row.get(15)));
        set(activity, "physicalIntensity", Short.parseShort(row.get(16)));
        ActivityInstruction instruction = new ActivityInstruction();
        set(instruction, "easierVariation", "Seeded easier variation");
        set(activity, "instruction", instruction);
        return activity;
    }

    private List<String> tokens(String tuple) {
        int start = tuple.indexOf('(') + 1;
        int end = tuple.lastIndexOf(')');
        List<String> result = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean quoted = false;
        for (int index = start; index < end; index++) {
            char current = tuple.charAt(index);
            if (current == '\'' && quoted && index + 1 < end && tuple.charAt(index + 1) == '\'') {
                token.append('\'');
                index++;
            } else if (current == '\'') {
                quoted = !quoted;
            } else if (current == ',' && !quoted) {
                result.add(token.toString().trim());
                token.setLength(0);
            } else {
                token.append(current);
            }
        }
        result.add(token.toString().trim());
        if (result.size() != 18) {
            throw new IllegalStateException("Expected 18 activity columns but parsed " + result.size());
        }
        return result;
    }

    private List<Scenario> scenarios() {
        return List.of(
                scenario("1 - first day, calm observer", 30, DunnQuadrant.C1, 2, 30, 24, Set.of()),
                scenario("2 - second day freshness elimination", 30, DunnQuadrant.C1, 2, 30, 24, Set.of(8L, 67L, 80L)),
                scenario("3 - short 15-minute budget", 30, DunnQuadrant.C1, 2, 15, 24, Set.of()),
                scenario("4 - energetic explorer", 40, DunnQuadrant.C2, 2, 30, 23, Set.of()),
                scenario("5 - sensitive observer", 30, DunnQuadrant.C3, 2, 30, 24, Set.of()),
                scenario("6 - high separation anxiety", 30, DunnQuadrant.C1, 4, 30, 22, Set.of()),
                scenario("7 - eight-month-old baby", 8, DunnQuadrant.C1, 2, 30, 24, Set.of()),
                scenario("8 - sixty-month-old child", 60, DunnQuadrant.C1, 2, 30, 31, Set.of()),
                scenario("9 - exact 48-month boundary", 48, DunnQuadrant.C1, 2, 30, 37, Set.of()),
                advancedLanguageScenario(),
                scenario("11 - protective profile content gap", 30, DunnQuadrant.C4, 2, 30, 2, Set.of()));
    }

    private Scenario advancedLanguageScenario() {
        return new Scenario("10 - learned language profile", 30, DunnQuadrant.C1, 2, 30, 24,
                Set.of(),
                (scores, levels) -> {
                    set(scores.get(IntelligenceType.VERBAL_LINGUISTIC), "score", new BigDecimal("4.20"));
                    set(scores.get(IntelligenceType.VERBAL_LINGUISTIC), "feedbackCount", 6);
                    set(scores.get(IntelligenceType.MUSICAL), "score", new BigDecimal("2.30"));
                    set(levels.get(DevelopmentDomain.LANGUAGE), "level", (short) 2);
                });
    }

    private Scenario scenario(String name, int age, DunnQuadrant quadrant, int anxiety, int budget,
                              int pool, Set<Long> yesterday) {
        return new Scenario(name, age, quadrant, anxiety, budget, pool,
                yesterday, ScenarioConfiguration.NONE);
    }

    private record Scenario(
            String name, int ageMonths, DunnQuadrant quadrant, int anxiety, int budgetMinutes,
            int expectedPoolSize, Set<Long> yesterday, ScenarioConfiguration configure) {
    }

    @FunctionalInterface
    private interface ScenarioConfiguration {
        ScenarioConfiguration NONE = (scores, levels) -> { };

        void apply(Map<IntelligenceType, ChildIntelligenceScore> scores,
                   Map<DevelopmentDomain, ChildDomainLevel> levels);
    }
}
