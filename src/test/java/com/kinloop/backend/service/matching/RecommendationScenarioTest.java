package com.kinloop.backend.service.matching;

import static com.kinloop.backend.service.matching.MatchingTestFixtures.levelOneDomains;
import static com.kinloop.backend.service.matching.MatchingTestFixtures.neutralIntelligenceScores;
import static com.kinloop.backend.service.matching.MatchingTestFixtures.parameters;
import static com.kinloop.backend.service.matching.MatchingTestFixtures.set;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Acceptance-level unit tests for the 15 August 2026 v5 recommendation examples.
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
    private final StrengthenCandidateSelector strengthenSelector = new StrengthenCandidateSelector();
    private final DailyPortfolioBuilder portfolioBuilder = new DailyPortfolioBuilder(new Random() {
        @Override
        public int nextInt(int bound) {
            return 0;
        }
    });
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
                .filter(activity -> eligibility.allows(activity, profile, parameters()))
                .toList();

        DevelopmentDomain period = periodFor(scenario.ageMonths());
        DunnProfile dunn = dunnProfile(scenario.quadrant());
        List<ScoredActivity> scored = pool.stream()
                .map(activity -> scorer.score(activity, profile, dunn, period,
                        intelligenceScores, domainLevels, scenario.yesterday(), parameters()))
                .sorted(Comparator.comparing(ScoredActivity::rawScore).reversed()
                        .thenComparing(value -> value.activity().getId()))
                .toList();

        List<DailyPortfolioBuilder.Selection> plan = buildPlan(
                scored, period, intelligenceScores, scenario.budgetMinutes());
        Map<PlanSlotType, DailyPortfolioBuilder.Selection> bySlot = plan.stream()
                .collect(Collectors.toMap(DailyPortfolioBuilder.Selection::slot, Function.identity()));
        Map<Long, DailyPortfolioBuilder.Selection> byActivityId = plan.stream()
                .collect(Collectors.toMap(item -> item.activity().activity().getId(), Function.identity()));

        assertAll(scenario.name(),
                () -> assertEquals(scenario.expectedPoolSize(), pool.size(), "eligible pool"),
                () -> assertEquals(scenario.expected().size(), plan.size(), "plan size"),
                () -> assertEquals(scenario.expected().keySet(), bySlot.keySet(), "plan slots"),
                () -> assertEquals(scenario.expected().values().stream().map(ExpectedActivity::id).collect(Collectors.toSet()),
                        byActivityId.keySet(), "selected activity ids"),
                () -> assertEquals(scenario.expectedMinutes(), plan.stream()
                        .mapToInt(item -> item.activity().activity().getDurationMinutes()).sum(),
                        "total minutes; selected=" + plan.stream()
                                .map(item -> item.slot() + ":" + item.activity().activity().getId())
                                .toList()));

        scenario.expected().forEach((documentedSlot, expected) -> {
            DailyPortfolioBuilder.Selection actual = byActivityId.get(expected.id());
            assertAll(documentedSlot + " activity " + expected.id(),
                    () -> assertBigDecimal(expected.rawScore(), actual.activity().rawScore(), "raw score"),
                    () -> assertBigDecimal(expected.displayScore(), actual.activity().displayScore(), "display score"),
                    () -> assertBreakdown(actual.activity(), expected));
        });
    }

    private List<DailyPortfolioBuilder.Selection> buildPlan(
            List<ScoredActivity> scored,
            DevelopmentDomain period,
            Map<IntelligenceType, ChildIntelligenceScore> intelligenceScores,
            int budget) {
        int leastSamples = intelligenceScores.values().stream()
                .mapToInt(ChildIntelligenceScore::getFeedbackCount).min().orElse(0);
        Set<IntelligenceType> leastKnown = intelligenceScores.values().stream()
                .filter(score -> score.getFeedbackCount() == leastSamples)
                .map(ChildIntelligenceScore::getIntelligenceType)
                .collect(Collectors.toSet());

        List<ScoredActivity> develop = scored.stream()
                .filter(value -> value.activity().getTargetDomain() == period).limit(5).toList();
        List<ScoredActivity> strengthen = strengthenSelector.select(scored, intelligenceScores, 5);
        List<ScoredActivity> explore = scored.stream()
                .filter(value -> leastKnown.contains(value.activity().getTargetIntelligence()))
                .limit(3).toList();
        return portfolioBuilder.build(develop, strengthen, explore, budget);
    }

    private void assertBreakdown(ScoredActivity actual, ExpectedActivity expected) {
        Map<String, Object> breakdown = actual.breakdown();
        assertAll("score breakdown",
                () -> assertDecimal(expected.distance(), negate(breakdown.get("D")), "D"),
                () -> assertDecimal(expected.gardner(), breakdown.get("G"), "G"),
                () -> assertDecimal(expected.period(), breakdown.get("P"), "P"),
                () -> assertDecimal(expected.zpd(), breakdown.get("Z"), "Z"),
                () -> assertDecimal(expected.attachment(), breakdown.get("B"), "B"),
                () -> assertDecimal(expected.freshness(), negate(breakdown.get("T")), "T"));
    }

    private BigDecimal negate(Object value) {
        return new BigDecimal(value.toString()).negate();
    }

    private void assertBigDecimal(BigDecimal expected, BigDecimal actual, String message) {
        assertEquals(0, expected.compareTo(actual), message);
    }

    private void assertDecimal(String expected, Object actual, String term) {
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual.toString())), term);
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
            case C4 -> tolerances(profile, 1, 2, 2, null, null, null);
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
        Map<PlanSlotType, ExpectedActivity> firstPlan = expected(
                e(PlanSlotType.DEVELOP, 8, "127", "-8", "0", "15", "20", "1", "0"),
                e(PlanSlotType.STRENGTHEN, 67, "124", "-11", "0", "15", "20", "1", "0"),
                e(PlanSlotType.EXPLORE, 80, "120", "0", "0", "0", "20", "1", "0"));
        return List.of(
                scenario("1 - first day, calm observer", 30, DunnQuadrant.C1, 2, 30, 24, 30, Set.of(), firstPlan),
                scenario("2 - second day freshness penalty", 30, DunnQuadrant.C1, 2, 30, 24, 30, Set.of(8L, 67L, 80L), expected(
                        e(PlanSlotType.DEVELOP, 74, "132", "-3", "0", "15", "20", "1", "0"),
                        e(PlanSlotType.STRENGTHEN, 59, "129", "-6", "0", "15", "20", "1", "0"))),
                scenario("3 - short 15-minute budget", 30, DunnQuadrant.C1, 2, 15, 24, 15, Set.of(), expected(
                        e(PlanSlotType.DEVELOP, 74, "132", "-3", "0", "15", "20", "1", "0"))),
                scenario("4 - energetic explorer", 40, DunnQuadrant.C2, 2, 30, 23, 30, Set.of(), expected(
                        e(PlanSlotType.DEVELOP, 102, "105", "-30", "0", "15", "20", "1", "0"),
                        e(PlanSlotType.STRENGTHEN, 104, "108", "-12", "0", "0", "20", "1", "0"),
                        e(PlanSlotType.EXPLORE, 86, "102", "-18", "0", "0", "20", "1", "0"))),
                scenario("5 - sensitive observer", 30, DunnQuadrant.C3, 2, 30, 24, 20, Set.of(), expected(
                        e(PlanSlotType.DEVELOP, 8, "119", "-16", "0", "15", "20", "1", "0"),
                        e(PlanSlotType.STRENGTHEN, 67, "113", "-22", "0", "15", "20", "1", "0"))),
                scenario("6 - high separation anxiety", 30, DunnQuadrant.C1, 4, 30, 22, 30, Set.of(), expected(
                        e(PlanSlotType.DEVELOP, 8, "146.05", "-8", "0", "15", "20", "1.15", "0"),
                        e(PlanSlotType.STRENGTHEN, 67, "142.60", "-11", "0", "15", "20", "1.15", "0"),
                        e(PlanSlotType.EXPLORE, 80, "138", "0", "0", "0", "20", "1.15", "0"))),
                scenario("7 - eight-month-old baby", 8, DunnQuadrant.C1, 2, 30, 24, 15, Set.of(), expected(
                        e(PlanSlotType.DEVELOP, 34, "110", "-5", "0", "15", "0", "1", "0"),
                        e(PlanSlotType.STRENGTHEN, 18, "107", "-8", "0", "15", "0", "1", "0"),
                        e(PlanSlotType.EXPLORE, 1, "105", "-10", "0", "15", "0", "1", "0"))),
                scenario("8 - sixty-month-old child", 60, DunnQuadrant.C1, 2, 30, 31, 30, Set.of(), expected(
                        e(PlanSlotType.DEVELOP, 14, "127", "-8", "0", "15", "20", "1", "0"),
                        e(PlanSlotType.STRENGTHEN, 118, "117", "-3", "0", "0", "20", "1", "0"))),
                scenario("9 - exact 48-month boundary", 48, DunnQuadrant.C1, 2, 30, 37, 30, Set.of(), expected(
                        e(PlanSlotType.DEVELOP, 95, "132", "-3", "0", "15", "20", "1", "0"),
                        e(PlanSlotType.STRENGTHEN, 11, "129", "-6", "0", "15", "20", "1", "0"))),
                advancedLanguageScenario(),
                scenario("11 - protective profile content gap", 30, DunnQuadrant.C4, 2, 30, 2, 0, Set.of(), Map.of()));
    }

    private Scenario advancedLanguageScenario() {
        return new Scenario("10 - learned language profile", 30, DunnQuadrant.C1, 2, 30, 24, 30,
                Set.of(), expected(
                        e(PlanSlotType.DEVELOP, 8, "117", "-8", "10", "15", "0", "1", "0"),
                        e(PlanSlotType.STRENGTHEN, 67, "114", "-11", "10", "15", "0", "1", "0"),
                        e(PlanSlotType.EXPLORE, 80, "120", "0", "0", "0", "20", "1", "0")),
                (scores, levels) -> {
                    set(scores.get(IntelligenceType.VERBAL_LINGUISTIC), "score", new BigDecimal("4.20"));
                    set(scores.get(IntelligenceType.VERBAL_LINGUISTIC), "feedbackCount", 6);
                    set(scores.get(IntelligenceType.MUSICAL), "score", new BigDecimal("2.30"));
                    set(levels.get(DevelopmentDomain.LANGUAGE), "level", (short) 2);
                });
    }

    private Scenario scenario(String name, int age, DunnQuadrant quadrant, int anxiety, int budget,
                              int pool, int minutes, Set<Long> yesterday,
                              Map<PlanSlotType, ExpectedActivity> expected) {
        return new Scenario(name, age, quadrant, anxiety, budget, pool, minutes,
                yesterday, expected, ScenarioConfiguration.NONE);
    }

    private Map<PlanSlotType, ExpectedActivity> expected(Map.Entry<PlanSlotType, ExpectedActivity>... entries) {
        Map<PlanSlotType, ExpectedActivity> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private Map.Entry<PlanSlotType, ExpectedActivity> e(
            PlanSlotType slot, long id, String raw, String d, String g, String p, String z, String b, String t) {
        return Map.entry(slot, new ExpectedActivity(id, new BigDecimal(raw), new BigDecimal("100"), d, g, p, z, b, t));
    }

    private record Scenario(
            String name, int ageMonths, DunnQuadrant quadrant, int anxiety, int budgetMinutes,
            int expectedPoolSize, int expectedMinutes, Set<Long> yesterday,
            Map<PlanSlotType, ExpectedActivity> expected, ScenarioConfiguration configure) {
    }

    private record ExpectedActivity(
            long id, BigDecimal rawScore, BigDecimal displayScore,
            String distance, String gardner, String period, String zpd, String attachment, String freshness) {
    }

    @FunctionalInterface
    private interface ScenarioConfiguration {
        ScenarioConfiguration NONE = (scores, levels) -> { };

        void apply(Map<IntelligenceType, ChildIntelligenceScore> scores,
                   Map<DevelopmentDomain, ChildDomainLevel> levels);
    }
}
