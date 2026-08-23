package com.kinloop.backend.service.matching;

import static com.kinloop.backend.service.matching.MatchingTestFixtures.set;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Acceptance scenarios executed against the complete Flyway-migrated activity
 * pool. The database, rather than a migration file parser, is the source of
 * activity content, Dunn profiles, age periods, and scoring parameters.
 */
@Testcontainers(disabledWithoutDocker = true)
class RecommendationScenarioTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("kinloop_recommendation_test")
                    .withUsername("kinloop")
                    .withPassword("kinloop");

    private static final LocalDate PLAN_DATE = LocalDate.of(2026, 8, 21);
    private static Connection connection;
    private static List<Activity> activities;
    private static Map<DunnQuadrant, DunnProfile> dunnProfiles;
    private static Map<String, BigDecimal> parameters;
    private static List<AgePeriod> agePeriods;

    private final ActivityEligibilityPolicy eligibility = new ActivityEligibilityPolicy();
    private final ActivityScorer scorer = new ActivityScorer();
    private final CandidateOrdering ordering = new CandidateOrdering();
    private final DailyPortfolioBuilder portfolioBuilder = new DailyPortfolioBuilder();

    @BeforeAll
    static void migrateAndLoadDatabase() throws SQLException {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        activities = loadActivities();
        dunnProfiles = loadDunnProfiles();
        parameters = loadParameters();
        agePeriods = loadAgePeriods();
    }

    @AfterAll
    static void closeConnection() throws SQLException {
        if (connection != null) connection.close();
    }

    @Test
    void loadsTheCompleteMigratedPoolInsteadOfParsingV10() {
        assertAll(
                () -> assertEquals(243, activities.size()),
                () -> assertEquals(243, activities.stream()
                        .filter(activity -> "HOME".equals(activity.getScope()))
                        .filter(activity -> "PUBLISHED".equals(activity.getStatus()))
                        .count()),
                () -> assertTrue(activities.stream().allMatch(activity -> activity.getInstruction() != null)),
                () -> assertTrue(activities.stream().allMatch(activity ->
                        activity.getInstruction().getEasierVariation() != null
                                && !activity.getInstruction().getEasierVariation().isBlank())),
                () -> assertTrue(activities.stream().allMatch(activity ->
                        activity.getInstruction().getHarderVariation() != null
                                && !activity.getInstruction().getHarderVariation().isBlank())));
    }

    @TestFactory
    Stream<DynamicTest> migratedV6ProfilesProduceUsablePlans() {
        return List.of(
                new Scenario("30 months calm", 30, DunnQuadrant.C1, 2, 35),
                new Scenario("30 months energetic", 30, DunnQuadrant.C2, 2, 35),
                new Scenario("30 months sensitive", 30, DunnQuadrant.C3, 2, 35),
                new Scenario("30 months protective", 30, DunnQuadrant.C4, 2, 35),
                new Scenario("30 months high anxiety", 30, DunnQuadrant.C1, 4, 35),
                new Scenario("8 month infant", 8, DunnQuadrant.C1, 2, 25),
                new Scenario("48 month boundary", 48, DunnQuadrant.C1, 2, 35),
                new Scenario("60 month child", 60, DunnQuadrant.C1, 2, 35)
        ).stream().map(scenario -> DynamicTest.dynamicTest(scenario.name(), () -> {
            ChildProfileSnapshot profile = profile(scenario.quadrant(), scenario.anxiety());
            List<Activity> pool = eligiblePool(scenario.ageMonths(), scenario.budgetMax(), profile);
            DailyPortfolioBuilder.Result plan = buildPlan(
                    pool, Set.of(), scenario.ageMonths(), scenario.budgetMax(), profile,
                    neutralScores(), initialLevels(scenario.ageMonths()), 41L, PLAN_DATE);

            assertTrue(pool.size() >= 3, "migrated eligible pool");
            assertEquals(3, plan.selections().size(), "complete v6 plan");
            assertTrue(plan.fallbackLevel() < 4, "must not use empty-pool fallback");
            if (scenario.quadrant() == DunnQuadrant.C4) {
                assertTrue(pool.size() > 2,
                        "the obsolete v5 C4 content-gap expectation must stay removed");
            }
            if (scenario.anxiety() >= 4) {
                assertTrue(pool.stream().noneMatch(activity ->
                        activity.getInvolvementType() == InvolvementType.BAGIMSIZ));
            }
        }));
    }

    @Test
    void appliesV6CeilingAndAttachmentScoringFromMigratedParameters() {
        Activity ceilingActivity = activities.stream()
                .filter(activity -> activity.getDifficulty() == 4)
                .findFirst().orElseThrow();
        Map<DevelopmentDomain, ChildDomainLevel> ceilingLevels = levelsAt((short) 4);
        ChildProfileSnapshot calm = profile(DunnQuadrant.C1, 2);
        ScoredActivity ceilingScore = scorer.score(
                ceilingActivity, calm, dunnProfiles.get(DunnQuadrant.C1), null,
                ceilingActivity.getTargetDomain(), neutralScores(), ceilingLevels, parameters);

        Activity together = activities.stream()
                .filter(activity -> activity.getInvolvementType() == InvolvementType.BIRLIKTE)
                .findFirst().orElseThrow();
        Activity supervised = activities.stream()
                .filter(activity -> activity.getInvolvementType() == InvolvementType.GOZETIMLI)
                .findFirst().orElseThrow();
        ChildProfileSnapshot anxious = profile(DunnQuadrant.C1, 4);
        Map<DevelopmentDomain, ChildDomainLevel> levels = levelsAt((short) 1);
        ScoredActivity togetherScore = scorer.score(
                together, anxious, dunnProfiles.get(DunnQuadrant.C1), null,
                together.getTargetDomain(), neutralScores(), levels, parameters);
        ScoredActivity supervisedScore = scorer.score(
                supervised, anxious, dunnProfiles.get(DunnQuadrant.C1), null,
                supervised.getTargetDomain(), neutralScores(), levels, parameters);

        assertAll(
                () -> assertDecimal(parameters.get("zpd_sweet_spot_bonus"),
                        ceilingScore.breakdown().get("Z")),
                () -> assertDecimal(parameters.get("attachment_multiplier_together"),
                        togetherScore.breakdown().get("B")),
                () -> assertDecimal(BigDecimal.ONE, supervisedScore.breakdown().get("B")));
    }

    @Test
    void fillsSlotsInOrderAndConsumesOnlyTheBudgetMaximum() {
        int age = 30;
        int budgetMax = 25;
        ChildProfileSnapshot profile = profile(DunnQuadrant.C1, 2);
        DailyPortfolioBuilder.Result plan = buildPlan(
                eligiblePool(age, budgetMax, profile), Set.of(), age, budgetMax, profile,
                neutralScores(), initialLevels(age), 71L, PLAN_DATE);

        assertAll(
                () -> assertEquals(List.of(
                                PlanSlotType.DEVELOP, PlanSlotType.STRENGTHEN, PlanSlotType.EXPLORE),
                        plan.selections().stream().map(DailyPortfolioBuilder.Selection::slot).toList()),
                () -> assertEquals(periodFor(age),
                        plan.selections().getFirst().activity().activity().getTargetDomain()),
                () -> assertEquals(3, plan.selections().stream()
                        .map(selection -> selection.activity().activity().getId()).distinct().count()),
                () -> assertTrue(plan.committedDurationMinutes() <= budgetMax),
                () -> assertTrue(plan.totalDurationMinutes() >= plan.committedDurationMinutes()),
                () -> assertTrue(plan.selections().stream()
                        .filter(selection -> !selection.withinBudget())
                        .allMatch(selection -> selection.slot() == PlanSlotType.EXPLORE)));
    }

    @Test
    void exercisesEveryV6FallbackLevelWithMigratedActivities() {
        int age = 30;
        int budget = 45;
        ChildProfileSnapshot profile = profile(DunnQuadrant.C1, 2);
        Map<IntelligenceType, ChildIntelligenceScore> scores = neutralScores();
        Map<DevelopmentDomain, ChildDomainLevel> levels = initialLevels(age);
        List<ScoredActivity> scored = scoredPool(
                eligiblePool(age, 10, profile), age, profile, scores, levels, 9L, PLAN_DATE);

        ScoredActivity development = scored.stream()
                .filter(candidate -> candidate.activity().getTargetDomain() == periodFor(age))
                .findFirst().orElseThrow();
        List<ScoredActivity> nonDevelopment = scored.stream()
                .filter(candidate -> candidate.activity().getTargetDomain() != periodFor(age))
                .limit(3).toList();
        assertEquals(3, nonDevelopment.size(), "fixture requires three non-development cards");

        Set<Long> fullIds = new LinkedHashSet<>();
        fullIds.add(development.activity().getId());
        nonDevelopment.stream().limit(2).map(candidate -> candidate.activity().getId()).forEach(fullIds::add);
        List<ScoredActivity> full = scored.stream()
                .filter(candidate -> fullIds.contains(candidate.activity().getId())).toList();
        List<ScoredActivity> freshWithoutDevelopment = full.stream()
                .filter(candidate -> candidate.activity().getId() != development.activity().getId()).toList();

        DailyPortfolioBuilder.Result fallbackOne = portfolioBuilder.build(new DailyPortfolioBuilder.Request(
                freshWithoutDevelopment, full, Set.of(development.activity().getId()),
                periodFor(age), scores, budget, false));
        DailyPortfolioBuilder.Result fallbackTwo = portfolioBuilder.build(new DailyPortfolioBuilder.Request(
                nonDevelopment, nonDevelopment, Set.of(), periodFor(age), scores, budget, false));
        List<ScoredActivity> twoCards = nonDevelopment.subList(0, 2);
        DailyPortfolioBuilder.Result fallbackThree = portfolioBuilder.build(new DailyPortfolioBuilder.Request(
                twoCards, twoCards, Set.of(), periodFor(age), scores, budget, false));
        DailyPortfolioBuilder.Result fallbackFour = portfolioBuilder.build(new DailyPortfolioBuilder.Request(
                List.of(), List.of(), Set.of(), periodFor(age), scores, budget, false));

        assertAll(
                () -> assertEquals((short) 1, fallbackOne.fallbackLevel()),
                () -> assertTrue(fallbackOne.selections().stream().anyMatch(
                        DailyPortfolioBuilder.Selection::repeatNotice)),
                () -> assertEquals((short) 2, fallbackTwo.fallbackLevel()),
                () -> assertEquals(3, fallbackTwo.selections().size()),
                () -> assertEquals((short) 3, fallbackThree.fallbackLevel()),
                () -> assertEquals(2, fallbackThree.selections().size()),
                () -> assertEquals((short) 4, fallbackFour.fallbackLevel()),
                () -> assertTrue(fallbackFour.selections().isEmpty()));
    }

    @Test
    void feedbackChangesGardnerAndDifficultyLedgersUsingMigratedParameters() {
        Activity activity = activities.stream()
                .filter(candidate -> candidate.getDifficulty() == 2)
                .findFirst().orElseThrow();
        Map<IntelligenceType, ChildIntelligenceScore> scores = neutralScores();
        ChildIntelligenceScore target = new ChildIntelligenceScore(
                1L, activity.getTargetIntelligence(), new BigDecimal("3.90"));
        scores.put(activity.getTargetIntelligence(), target);
        ChildIntelligenceScore secondary = scores.get(activity.getSecondaryIntelligence());
        Map<DevelopmentDomain, ChildDomainLevel> levels = levelsAt((short) 1);
        ChildDomainLevel domain = levels.get(activity.getTargetDomain());
        ChildProfileSnapshot profile = profile(DunnQuadrant.C1, 2);

        ScoredActivity before = scorer.score(
                activity, profile, dunnProfiles.get(DunnQuadrant.C1), null,
                activity.getTargetDomain(), scores, levels, parameters);
        target.applyFeedback(parameters.get("liked_target_delta"),
                parameters.get("gardner_runtime_min_score"),
                parameters.get("gardner_runtime_max_score"));
        target.recordFeedbackSample();
        secondary.applyFeedback(parameters.get("liked_secondary_delta"),
                parameters.get("gardner_runtime_min_score"),
                parameters.get("gardner_runtime_max_score"));
        ScoredActivity after = scorer.score(
                activity, profile, dunnProfiles.get(DunnQuadrant.C1), null,
                activity.getTargetDomain(), scores, levels, parameters);

        applyDomainFeedback(domain, parameters.get("level_credit_stretch"));
        applyDomainFeedback(domain, parameters.get("level_credit_stretch"));
        applyDomainFeedback(domain, parameters.get("level_credit_stretch"));
        assertEquals((short) 2, domain.getLevel(), "three stretch credits level up");
        applyDomainFeedback(domain, parameters.get("level_penalty_struggle_at_level"));
        applyDomainFeedback(domain, parameters.get("level_penalty_struggle_at_level"));

        assertAll(
                () -> assertDecimal(new BigDecimal("4.20"), target.getScore()),
                () -> assertDecimal(new BigDecimal("3.15"), secondary.getScore()),
                () -> assertEquals(1, target.getFeedbackCount()),
                () -> assertDecimal(BigDecimal.ZERO, before.breakdown().get("G")),
                () -> assertDecimal(parameters.get("gardner_comfort_bonus"),
                        after.breakdown().get("G")),
                () -> assertEquals((short) 1, domain.getLevel(),
                        "two at-level struggle votes level down"));
    }

    @Test
    void identicalDatabaseProfileAndDateProduceTheSamePlanEveryTime() {
        int age = 30;
        int budget = 35;
        long childId = 812L;
        ChildProfileSnapshot profile = profile(DunnQuadrant.C1, 2);
        List<Activity> pool = eligiblePool(age, budget, profile);
        Map<IntelligenceType, ChildIntelligenceScore> scores = neutralScores();
        Map<DevelopmentDomain, ChildDomainLevel> levels = initialLevels(age);

        List<Long> expected = planIds(buildPlan(
                pool, Set.of(), age, budget, profile, scores, levels, childId, PLAN_DATE));
        for (int attempt = 0; attempt < 10; attempt++) {
            assertEquals(expected, planIds(buildPlan(
                    pool, Set.of(), age, budget, profile, scores, levels, childId, PLAN_DATE)));
        }
    }

    private DailyPortfolioBuilder.Result buildPlan(
            List<Activity> pool,
            Set<Long> recentIds,
            int age,
            int budgetMax,
            ChildProfileSnapshot profile,
            Map<IntelligenceType, ChildIntelligenceScore> scores,
            Map<DevelopmentDomain, ChildDomainLevel> levels,
            long childId,
            LocalDate planDate
    ) {
        List<ScoredActivity> preFreshness = scoredPool(
                pool, age, profile, scores, levels, childId, planDate);
        List<ScoredActivity> fresh = preFreshness.stream()
                .filter(candidate -> !recentIds.contains(candidate.activity().getId()))
                .toList();
        boolean supervisedGuarantee = profile.getSeparationAnxiety() != null
                && profile.getSeparationAnxiety() >= parameters.get("attachment_anxiety_threshold").intValueExact()
                && parameters.get("attachment_guarantee_supervised").signum() != 0;
        return portfolioBuilder.build(new DailyPortfolioBuilder.Request(
                fresh, preFreshness, recentIds, periodFor(age), scores, budgetMax,
                supervisedGuarantee));
    }

    private List<ScoredActivity> scoredPool(
            List<Activity> pool,
            int age,
            ChildProfileSnapshot profile,
            Map<IntelligenceType, ChildIntelligenceScore> scores,
            Map<DevelopmentDomain, ChildDomainLevel> levels,
            long childId,
            LocalDate planDate
    ) {
        DunnProfile dunn = dunnProfiles.get(profile.getDunnQuadrant());
        DevelopmentDomain period = periodFor(age);
        return pool.stream()
                .map(activity -> scorer.score(
                        activity, profile, dunn, null, period, scores, levels, parameters))
                .sorted(ordering.comparator(childId, planDate, scores, parameters))
                .toList();
    }

    private List<Activity> eligiblePool(int age, int budgetMax, ChildProfileSnapshot profile) {
        return activities.stream()
                .filter(activity -> "HOME".equals(activity.getScope()))
                .filter(activity -> "PUBLISHED".equals(activity.getStatus()))
                .filter(activity -> activity.getDeletedAt() == null)
                .filter(activity -> activity.getMinAgeMonths() <= age)
                .filter(activity -> activity.getMaxAgeMonths() >= age)
                .filter(activity -> activity.getDurationMinutes() <= budgetMax)
                .filter(activity -> eligibility.allows(activity, profile, null, parameters))
                .toList();
    }

    private static ChildProfileSnapshot profile(DunnQuadrant quadrant, int anxiety) {
        ChildProfileSnapshot profile = new ChildProfileSnapshot();
        profile.setDunnQuadrant(quadrant);
        profile.setSeparationAnxiety(anxiety);
        profile.setFocusBand(FocusBand.MEDIUM);
        return profile;
    }

    private static Map<IntelligenceType, ChildIntelligenceScore> neutralScores() {
        Map<IntelligenceType, ChildIntelligenceScore> scores = new EnumMap<>(IntelligenceType.class);
        for (IntelligenceType type : IntelligenceType.values()) {
            scores.put(type, new ChildIntelligenceScore(1L, type, new BigDecimal("3.00")));
        }
        return scores;
    }

    private static Map<DevelopmentDomain, ChildDomainLevel> initialLevels(int ageMonths) {
        short level = ageMonths < 48 ? (short) 1 : ageMonths < 60 ? (short) 2 : (short) 3;
        return levelsAt(level);
    }

    private static Map<DevelopmentDomain, ChildDomainLevel> levelsAt(short level) {
        Map<DevelopmentDomain, ChildDomainLevel> levels = new EnumMap<>(DevelopmentDomain.class);
        for (DevelopmentDomain domain : DevelopmentDomain.values()) {
            levels.put(domain, new ChildDomainLevel(1L, domain, level));
        }
        return levels;
    }

    private void applyDomainFeedback(ChildDomainLevel level, BigDecimal delta) {
        level.applyFeedback(
                delta,
                parameters.get("level_up_threshold"),
                parameters.get("level_down_threshold"),
                parameters.get("level_min").shortValueExact(),
                parameters.get("level_max").shortValueExact(),
                parameters.get("ceiling_counter_cap"));
    }

    private static DevelopmentDomain periodFor(int ageMonths) {
        return agePeriods.stream()
                .filter(period -> period.minAgeMonths() <= ageMonths)
                .filter(period -> period.maxAgeMonths() > ageMonths)
                .map(AgePeriod::domain)
                .findFirst().orElseThrow();
    }

    private static List<Long> planIds(DailyPortfolioBuilder.Result plan) {
        return plan.selections().stream()
                .map(selection -> selection.activity().activity().getId())
                .toList();
    }

    private static void assertDecimal(BigDecimal expected, Object actual) {
        assertTrue(actual instanceof BigDecimal, "expected a BigDecimal but got " + actual);
        assertEquals(0, expected.compareTo((BigDecimal) actual));
    }

    private static List<Activity> loadActivities() throws SQLException {
        List<Activity> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT a.id, a.scope, a.status, a.title, a.description,
                       a.min_age_months, a.max_age_months,
                       a.target_intelligence, a.secondary_intelligence, a.target_domain,
                       a.difficulty, a.duration_minutes, a.involvement_type,
                       a.noise_load, a.visual_load, a.physical_intensity, a.deleted_at,
                       i.easier_variation, i.harder_variation
                FROM activities a
                LEFT JOIN activity_instructions i ON i.activity_id = a.id
                ORDER BY a.id
                """);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                Activity activity = new Activity();
                set(activity, "id", rows.getLong("id"));
                set(activity, "scope", rows.getString("scope"));
                set(activity, "status", rows.getString("status"));
                set(activity, "title", rows.getString("title"));
                set(activity, "description", rows.getString("description"));
                set(activity, "minAgeMonths", rows.getInt("min_age_months"));
                set(activity, "maxAgeMonths", rows.getInt("max_age_months"));
                set(activity, "targetIntelligence",
                        IntelligenceType.valueOf(rows.getString("target_intelligence")));
                set(activity, "secondaryIntelligence",
                        IntelligenceType.valueOf(rows.getString("secondary_intelligence")));
                set(activity, "targetDomain",
                        DevelopmentDomain.valueOf(rows.getString("target_domain")));
                set(activity, "difficulty", rows.getShort("difficulty"));
                set(activity, "durationMinutes", rows.getShort("duration_minutes"));
                set(activity, "involvementType",
                        InvolvementType.valueOf(rows.getString("involvement_type")));
                set(activity, "noiseLoad", rows.getShort("noise_load"));
                set(activity, "visualLoad", rows.getShort("visual_load"));
                set(activity, "physicalIntensity", rows.getShort("physical_intensity"));
                set(activity, "deletedAt", rows.getObject("deleted_at", java.time.OffsetDateTime.class));
                ActivityInstruction instruction = new ActivityInstruction();
                set(instruction, "easierVariation", rows.getString("easier_variation"));
                set(instruction, "harderVariation", rows.getString("harder_variation"));
                set(activity, "instruction", instruction);
                result.add(activity);
            }
        }
        return List.copyOf(result);
    }

    private static Map<DunnQuadrant, DunnProfile> loadDunnProfiles() throws SQLException {
        Map<DunnQuadrant, DunnProfile> result = new EnumMap<>(DunnQuadrant.class);
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT quadrant, noise_tolerance, visual_tolerance, movement_tolerance,
                       noise_weight, visual_weight, movement_weight
                FROM dunn_profiles
                """);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                DunnQuadrant quadrant = DunnQuadrant.valueOf(rows.getString("quadrant"));
                DunnProfile profile = new DunnProfile();
                set(profile, "quadrant", quadrant);
                set(profile, "noiseTolerance", rows.getShort("noise_tolerance"));
                set(profile, "visualTolerance", rows.getShort("visual_tolerance"));
                set(profile, "movementTolerance", rows.getShort("movement_tolerance"));
                set(profile, "noiseWeight", rows.getBigDecimal("noise_weight"));
                set(profile, "visualWeight", rows.getBigDecimal("visual_weight"));
                set(profile, "movementWeight", rows.getBigDecimal("movement_weight"));
                result.put(quadrant, profile);
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, BigDecimal> loadParameters() throws SQLException {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT parameter_key, value FROM scoring_parameters ORDER BY parameter_key
                """);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) result.put(rows.getString(1), rows.getBigDecimal(2));
        }
        return Map.copyOf(result);
    }

    private static List<AgePeriod> loadAgePeriods() throws SQLException {
        List<AgePeriod> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT min_age_months, max_age_months, target_domain
                FROM developmental_period_tasks
                ORDER BY min_age_months
                """);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(new AgePeriod(
                        rows.getInt("min_age_months"),
                        rows.getInt("max_age_months"),
                        DevelopmentDomain.valueOf(rows.getString("target_domain"))));
            }
        }
        return List.copyOf(result);
    }

    private record Scenario(
            String name, int ageMonths, DunnQuadrant quadrant, int anxiety, int budgetMax) {
    }

    private record AgePeriod(int minAgeMonths, int maxAgeMonths, DevelopmentDomain domain) {
    }
}
