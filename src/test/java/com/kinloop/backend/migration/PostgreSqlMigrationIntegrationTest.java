package com.kinloop.backend.migration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("kinloop_migration_test")
                    .withUsername("kinloop")
                    .withPassword("kinloop");

    private static Connection connection;
    private static int migrationsExecuted;

    @BeforeAll
    static void migrateEmptyDatabase() throws SQLException {
        Flyway throughV19 = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target("19")
                .load();
        migrationsExecuted = throughV19.migrate().migrationsExecuted;

        try (Connection setup = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             PreparedStatement statement = setup.prepareStatement("""
                     WITH created_user AS (
                         INSERT INTO users (email, password_hash, role)
                         VALUES ('migration-test@kinloop.local', 'not-used', 'PARENT')
                         RETURNING id
                     ), created_parent AS (
                         INSERT INTO parent_profiles (user_id, daily_time_budget_minutes)
                         SELECT id, 10 FROM created_user
                         RETURNING id
                     )
                     INSERT INTO children (parent_id, birth_date)
                     SELECT id, DATE '2023-01-01' FROM created_parent
                     """)) {
            assertEquals(1, statement.executeUpdate());
        }

        Flyway throughLatest = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();
        migrationsExecuted += throughLatest.migrate().migrationsExecuted;
        connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    @AfterAll
    static void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void appliesEveryMigrationThroughV38() throws SQLException {
        MigrationHistory history = queryOne("""
                SELECT count(*) AS migration_count,
                       min(version::integer) AS first_version,
                       max(version::integer) AS last_version,
                       bool_and(success) AS all_successful
                FROM flyway_schema_history
                WHERE type = 'SQL'
                """, result -> new MigrationHistory(
                result.getInt("migration_count"),
                result.getInt("first_version"),
                result.getInt("last_version"),
                result.getBoolean("all_successful")));

        assertAll(
                () -> assertEquals(38, migrationsExecuted),
                () -> assertEquals(38, history.migrationCount()),
                () -> assertEquals(1, history.firstVersion()),
                () -> assertEquals(38, history.lastVersion()),
                () -> assertTrue(history.allSuccessful()));
    }

    @Test
    void allowsOnlyOneUnfinishedSelectionPerDailyPlan() throws SQLException {
        String indexDefinition = queryOne("""
                SELECT indexdef
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'uq_daily_plan_items_selected_per_plan'
                """, result -> result.getString("indexdef"));

        assertAll(
                () -> assertTrue(indexDefinition.contains("selected_at IS NOT NULL")),
                () -> assertTrue(indexDefinition.contains("completed_at IS NULL")));
    }

    @Test
    void removesOtherGenderIdentityOption() throws SQLException {
        int otherOptions = queryInt("""
                SELECT count(*)
                FROM question_options qo
                JOIN questions q ON q.id = qo.question_id
                WHERE q.code = 'Q8'
                  AND qo.code = 'OTHER'
                """);
        int undisclosedDisplayOrder = queryInt("""
                SELECT qo.display_order
                FROM question_options qo
                JOIN questions q ON q.id = qo.question_id
                WHERE q.code = 'Q8'
                  AND qo.code = 'UNDISCLOSED'
                """);

        assertAll(
                () -> assertEquals(0, otherOptions),
                () -> assertEquals(3, undisclosedDisplayOrder));
    }

    @Test
    void removesTransitionalV5BudgetMappingsScopesAndParameters() throws SQLException {
        int oldBudgetColumns = queryInt("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name IN ('parent_profiles', 'children', 'question_options')
                  AND column_name = 'daily_time_budget_minutes'
                """);
        int householdQuestions = queryInt("SELECT count(*) FROM questions WHERE scope = 'HOUSEHOLD'");
        int obsoleteParameters = queryInt("""
                SELECT count(*)
                FROM scoring_parameters
                WHERE parameter_key IN (
                    'freshness_penalty',
                    'freshness_lookback_days',
                    'attachment_multiplier',
                    'high_separation_anxiety_threshold',
                    'slot_candidate_limit',
                    'explore_random_top_limit',
                    'domain_initial_level',
                    'harder_variation_streak_increment',
                    'normal_variation_streak_increment',
                    'easier_variation_streak_increment',
                    'domain_level_up_streak_threshold',
                    'domain_level_down_easier_threshold')
                """);
        String scopeConstraint = constraint("questions", "chk_questions_scope");

        assertAll(
                () -> assertEquals(0, oldBudgetColumns),
                () -> assertEquals(0, householdQuestions),
                () -> assertEquals(0, obsoleteParameters),
                () -> assertFalse(scopeConstraint.contains("HOUSEHOLD")),
                () -> assertTrue(scopeConstraint.contains("CHILD_BUDGET")));
    }

    @Test
    void movesBudgetMappingsToRangesAndAddsClosingMessageState() throws SQLException {
        Map<String, ColumnMetadata> optionColumns = columns(
                "question_options", "daily_time_budget_min", "daily_time_budget_max");
        Map<String, ColumnMetadata> childColumns = columns(
                "children", "onboarding_closing_message_responded_at",
                "onboarding_closing_reminder_requested",
                "onboarding_closing_reminder_plan_baseline");
        int householdBudgetColumns = queryInt("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'parent_profiles'
                  AND column_name = 'daily_time_budget_minutes'
                """);
        int validRanges = queryInt("""
                SELECT count(*)
                FROM question_options qo
                JOIN questions q ON q.id = qo.question_id
                WHERE q.code = 'Q7'
                  AND (qo.daily_time_budget_min, qo.daily_time_budget_max)
                      IN ((15,25), (25,35), (35,45))
                """);

        assertAll(
                () -> assertEquals(0, householdBudgetColumns),
                () -> assertEquals(3, validRanges),
                () -> assertEquals("smallint", optionColumns.get("daily_time_budget_min").dataType()),
                () -> assertEquals("smallint", optionColumns.get("daily_time_budget_max").dataType()),
                () -> assertEquals("timestamp with time zone",
                        childColumns.get("onboarding_closing_message_responded_at").dataType()),
                () -> assertRequiredColumn(childColumns,
                        "onboarding_closing_reminder_requested", "boolean", "false"),
                () -> assertEquals("integer",
                        childColumns.get("onboarding_closing_reminder_plan_baseline").dataType()),
                () -> assertEquals("YES",
                        childColumns.get("onboarding_closing_reminder_plan_baseline").nullable()));
    }

    @Test
    void feedbackEffectsHaveNullableReversalTimestamp() throws SQLException {
        ColumnMetadata reversedAt = column("feedback_effects", "reversed_at");

        assertAll(
                () -> assertEquals("timestamp with time zone", reversedAt.dataType()),
                () -> assertEquals("YES", reversedAt.nullable()),
                () -> assertNull(reversedAt.columnDefault()));
    }

    @Test
    void createsOptionalChildSensoryAdjustments() throws SQLException {
        Map<String, ColumnMetadata> columns = columns(
                "child_sensory_adjustments",
                "child_id", "noise_adjustment", "visual_adjustment", "movement_adjustment",
                "involvement_filter", "updated_at");

        assertAll(
                () -> assertEquals("bigint", columns.get("child_id").dataType()),
                () -> assertRequiredColumn(columns, "noise_adjustment", "smallint", "0"),
                () -> assertRequiredColumn(columns, "visual_adjustment", "smallint", "0"),
                () -> assertRequiredColumn(columns, "movement_adjustment", "smallint", "0"),
                () -> assertEquals("YES", columns.get("involvement_filter").nullable()),
                () -> assertRequiredColumn(
                        columns, "updated_at", "timestamp with time zone", "now()"),
                () -> assertTrue(constraint(
                        "child_sensory_adjustments",
                        "chk_child_sensory_adjustments_involvement_filter").contains("RELAXED")));
    }

    @Test
    void addsNullableFeedbackFreeTextColumn() throws SQLException {
        ColumnMetadata freeText = column("feedback", "free_text");
        int configuredMaxLength = queryInt("""
                SELECT max_length
                FROM questions
                WHERE code = 'FB_COMMENT'
                  AND scope = 'FEEDBACK'
                """);
        String lengthConstraint = constraint("feedback", "chk_feedback_free_text_length");

        assertAll(
                () -> assertEquals("text", freeText.dataType()),
                () -> assertEquals("YES", freeText.nullable()),
                () -> assertNull(freeText.columnDefault()),
                () -> assertEquals(500, configuredMaxLength),
                () -> assertTrue(lengthConstraint.contains("char_length(free_text) <= 500")));
    }

    @Test
    void createsFeedbackLlmClassificationStorage() throws SQLException {
        Map<String, ColumnMetadata> columns = columns(
                "feedback_llm_classifications",
                "feedback_id", "applied", "confidence", "target_correction", "secondary_hint",
                "sensory_hint", "involvement_hint", "difficulty_hint", "situation_hint",
                "duration_hint", "conflict", "raw_response", "created_at");
        ColumnMetadata confidence = columns.get("confidence");

        assertAll(
                () -> assertEquals("bigint", columns.get("feedback_id").dataType()),
                () -> assertRequiredColumn(columns, "applied", "boolean", "false"),
                () -> assertEquals("numeric", confidence.dataType()),
                () -> assertEquals(3, confidence.numericPrecision()),
                () -> assertEquals(2, confidence.numericScale()),
                () -> assertEquals("YES", confidence.nullable()),
                () -> assertEquals("character varying", columns.get("target_correction").dataType()),
                () -> assertEquals("character varying", columns.get("secondary_hint").dataType()),
                () -> assertEquals("character varying", columns.get("sensory_hint").dataType()),
                () -> assertEquals("character varying", columns.get("involvement_hint").dataType()),
                () -> assertEquals("character varying", columns.get("difficulty_hint").dataType()),
                () -> assertEquals("character varying", columns.get("situation_hint").dataType()),
                () -> assertEquals("character varying", columns.get("duration_hint").dataType()),
                () -> assertRequiredColumn(columns, "conflict", "boolean", "false"),
                () -> assertEquals("text", columns.get("raw_response").dataType()),
                () -> assertRequiredColumn(columns, "created_at", "timestamp with time zone", "now()"),
                () -> assertTrue(constraint("feedback_llm_classifications",
                        "chk_feedback_llm_sensory_hint").contains("CROWDING")),
                () -> assertTrue(constraint("feedback_llm_classifications",
                        "chk_feedback_llm_involvement_hint").contains("TOGETHER")),
                () -> assertTrue(constraint("feedback_llm_classifications",
                        "chk_feedback_llm_difficulty_hint").contains("HARDER")),
                () -> assertTrue(constraint("feedback_llm_classifications",
                        "chk_feedback_llm_situation_hint").contains("TRANSIENT")),
                () -> assertTrue(constraint("feedback_llm_classifications",
                        "chk_feedback_llm_duration_hint").contains("SHORT")));
    }

    @Test
    void simplifiesActivityFeedbackQuestions() throws SQLException {
        int removedQuestionCount = queryInt("SELECT count(*) FROM questions WHERE id = 14");
        int removedOptionCount = queryInt("SELECT count(*) FROM question_options WHERE question_id = 14");
        Map<String, String> options = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT code, label
                FROM question_options
                WHERE question_id = 13
                ORDER BY display_order
                """);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                options.put(result.getString("code"), result.getString("label"));
            }
        }

        assertAll(
                () -> assertEquals(0, removedQuestionCount),
                () -> assertEquals(0, removedOptionCount),
                () -> assertEquals(Map.of(
                        "LIKED", "Yaptık, sevdi",
                        "STRUGGLED", "Denedik, zorlandı",
                        "DISLIKED", "Olmadı, sevmedi"), options));
    }

    @Test
    void loadsTheComplete243ActivityPool() throws SQLException {
        ActivityPool pool = queryOne("""
                SELECT count(*) AS activity_count,
                       min(id) AS first_id,
                       max(id) AS last_id,
                       count(*) FILTER (
                           WHERE scope = 'HOME'
                             AND status = 'PUBLISHED'
                             AND deleted_at IS NULL
                       ) AS active_home_count,
                       count(DISTINCT id) AS distinct_id_count
                FROM activities
                """, result -> new ActivityPool(
                result.getInt("activity_count"),
                result.getLong("first_id"),
                result.getLong("last_id"),
                result.getInt("active_home_count"),
                result.getInt("distinct_id_count")));

        int instructionCount = queryInt("SELECT count(*) FROM activity_instructions");
        int activitiesWithAtLeastFourSteps = queryInt("""
                SELECT count(*)
                FROM (
                    SELECT activity_id
                    FROM activity_steps
                    GROUP BY activity_id
                    HAVING count(*) >= 4
                ) values_with_steps
                """);
        int activitiesWithAtLeastThreeOutcomes = queryInt("""
                SELECT count(*)
                FROM (
                    SELECT activity_id
                    FROM activity_outcomes
                    GROUP BY activity_id
                    HAVING count(*) >= 3
                ) values_with_outcomes
                """);
        int publicationReadyActivities = queryInt("""
                SELECT count(*)
                FROM activities a
                JOIN activity_instructions i ON i.activity_id = a.id
                WHERE a.status = 'PUBLISHED'
                  AND a.secondary_intelligence IS NOT NULL
                  AND a.secondary_intelligence <> a.target_intelligence
                  AND btrim(i.easier_variation) <> ''
                  AND btrim(i.harder_variation) <> ''
                  AND a.difficulty BETWEEN 1 AND 4
                  AND a.duration_minutes > 0
                """);

        assertAll(
                () -> assertEquals(243, pool.activityCount()),
                () -> assertEquals(1L, pool.firstId()),
                () -> assertEquals(243L, pool.lastId()),
                () -> assertEquals(243, pool.activeHomeCount()),
                () -> assertEquals(243, pool.distinctIdCount()),
                () -> assertEquals(243, instructionCount),
                () -> assertEquals(243, activitiesWithAtLeastFourSteps),
                () -> assertEquals(243, activitiesWithAtLeastThreeOutcomes),
                () -> assertEquals(243, publicationReadyActivities),
                () -> assertFalse(columnExists("activities", "is_scaffolded")),
                () -> assertFalse(relationExists("v_scorable_activities")));
    }

    @Test
    void blocksPublishingUntilAllContentRequirementsAreMet() throws SQLException {
        long activityId = insertReturningLong("""
                INSERT INTO activities (
                    scope, status, title, min_age_months, max_age_months,
                    target_intelligence, secondary_intelligence, target_domain,
                    difficulty, duration_minutes, involvement_type,
                    noise_load, visual_load, physical_intensity
                ) VALUES (
                    'HOME', 'DRAFT', 'Publication gate test', 24, 48,
                    'MUSICAL', NULL, 'COGNITIVE', 2, 10, 'BIRLIKTE', 1, 1, 1
                )
                RETURNING id
                """);

        try {
            executeUpdate("""
                    INSERT INTO activity_instructions (
                        activity_id, easier_variation, harder_variation
                    ) VALUES (?, 'Make it easier', 'Make it harder')
                    """, activityId);
            executeUpdate("""
                    INSERT INTO activity_steps (activity_id, step_no, text) VALUES
                        (?, 1, 'Step one'), (?, 2, 'Step two'),
                        (?, 3, 'Step three'), (?, 4, 'Step four')
                    """, activityId, activityId, activityId, activityId);
            executeUpdate("""
                    INSERT INTO activity_outcomes (activity_id, outcome, display_order) VALUES
                        (?, 'Outcome one', 1), (?, 'Outcome two', 2), (?, 'Outcome three', 3)
                    """, activityId, activityId, activityId);

            assertPublicationRejected(activityId, "secondary_intelligence is required");

            executeUpdate("UPDATE activities SET secondary_intelligence = 'NATURALISTIC' WHERE id = ?",
                    activityId);
            executeUpdate("UPDATE activity_instructions SET easier_variation = '  ' WHERE activity_id = ?",
                    activityId);
            assertPublicationRejected(activityId, "easier_variation is required");

            executeUpdate("""
                    UPDATE activity_instructions
                    SET easier_variation = 'Make it easier', harder_variation = ''
                    WHERE activity_id = ?
                    """, activityId);
            assertPublicationRejected(activityId, "harder_variation is required");

            executeUpdate("""
                    UPDATE activity_instructions SET harder_variation = 'Make it harder'
                    WHERE activity_id = ?
                    """, activityId);
            executeUpdate("DELETE FROM activity_steps WHERE activity_id = ? AND step_no = 4", activityId);
            assertPublicationRejected(activityId, "at least 4 steps are required");

            executeUpdate("""
                    INSERT INTO activity_steps (activity_id, step_no, text)
                    VALUES (?, 4, 'Step four')
                    """, activityId);
            executeUpdate("DELETE FROM activity_outcomes WHERE activity_id = ? AND display_order = 3",
                    activityId);
            assertPublicationRejected(activityId, "at least 3 outcomes are required");

            executeUpdate("""
                    INSERT INTO activity_outcomes (activity_id, outcome, display_order)
                    VALUES (?, 'Outcome three', 3)
                    """, activityId);
            assertEquals(1, executeUpdate("UPDATE activities SET status = 'PUBLISHED' WHERE id = ?",
                    activityId));

            String validationFunction = queryOne("""
                    SELECT pg_get_functiondef('validate_published_activity()'::regprocedure)
                    """, result -> result.getString(1));
            assertAll(
                    () -> assertTrue(validationFunction.contains("target_domain NOT IN")),
                    () -> assertTrue(validationFunction.contains("target_intelligence NOT IN")),
                    () -> assertTrue(validationFunction.contains("target_intelligence = NEW.secondary_intelligence")),
                    () -> assertTrue(validationFunction.contains("difficulty NOT BETWEEN 1 AND 4")),
                    () -> assertTrue(validationFunction.contains("duration_minutes IS NULL")),
                    () -> assertEquals(1, queryInt("""
                            SELECT count(*)
                            FROM pg_trigger
                            WHERE tgrelid = 'activities'::regclass
                              AND tgname = 'trg_activities_validate_published'
                              AND NOT tgisinternal
                            """)));
        } finally {
            executeUpdate("DELETE FROM activities WHERE id = ?", activityId);
        }
    }

    @Test
    void alignsC4WeightsAndTheFinalDevelopmentalAgeBand() throws SQLException {
        DunnProfile c4 = queryOne("""
                SELECT noise_tolerance, visual_tolerance, movement_tolerance,
                       noise_weight, visual_weight, movement_weight
                FROM dunn_profiles
                WHERE quadrant = 'C4'
                """, result -> new DunnProfile(
                result.getInt("noise_tolerance"),
                result.getInt("visual_tolerance"),
                result.getInt("movement_tolerance"),
                result.getBigDecimal("noise_weight"),
                result.getBigDecimal("visual_weight"),
                result.getBigDecimal("movement_weight")));

        int maxAge = queryInt("""
                SELECT max_age_months
                FROM developmental_period_tasks
                WHERE min_age_months = 48
                  AND target_domain = 'SOCIAL_EMOTIONAL'
                """);

        assertAll(
                () -> assertEquals(1, c4.noiseTolerance()),
                () -> assertEquals(2, c4.visualTolerance()),
                () -> assertEquals(2, c4.movementTolerance()),
                () -> assertDecimal("5", c4.noiseWeight()),
                () -> assertDecimal("5", c4.visualWeight()),
                () -> assertDecimal("3", c4.movementWeight()),
                () -> assertEquals("NO", column("dunn_profiles", "noise_weight").nullable()),
                () -> assertEquals("NO", column("dunn_profiles", "visual_weight").nullable()),
                () -> assertEquals("NO", column("dunn_profiles", "movement_weight").nullable()),
                () -> assertEquals(73, maxAge));
    }

    @Test
    void createsChildBudgetRangesAndDecimalSignedStreaks() throws SQLException {
        ColumnMetadata budgetMin = column("children", "daily_time_budget_min");
        ColumnMetadata budgetMax = column("children", "daily_time_budget_max");
        ColumnMetadata streak = column("child_domain_levels", "streak");
        String budgetConstraint = constraint("children", "chk_children_time_budget_range");
        int backfilledChildren = queryInt("""
                SELECT count(*)
                FROM children
                WHERE daily_time_budget_min = 25
                  AND daily_time_budget_max = 35
                """);

        assertAll(
                () -> assertEquals("smallint", budgetMin.dataType()),
                () -> assertEquals("NO", budgetMin.nullable()),
                () -> assertNull(budgetMin.columnDefault()),
                () -> assertEquals("smallint", budgetMax.dataType()),
                () -> assertEquals("NO", budgetMax.nullable()),
                () -> assertNull(budgetMax.columnDefault()),
                () -> assertEquals(1, backfilledChildren),
                () -> assertTrue(budgetConstraint.contains("15")
                        && budgetConstraint.contains("25")
                        && budgetConstraint.contains("35")
                        && budgetConstraint.contains("45")),
                () -> assertEquals("numeric", streak.dataType()),
                () -> assertEquals(4, streak.numericPrecision()),
                () -> assertEquals(1, streak.numericScale()),
                () -> assertEquals("NO", streak.nullable()),
                () -> assertTrue(streak.columnDefault().startsWith("0")));
    }

    @Test
    void installsEveryV6ScoringParameter() throws SQLException {
        Map<String, BigDecimal> expected = expectedV6Parameters();
        Map<String, BigDecimal> actual = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT parameter_key, value
                FROM scoring_parameters
                WHERE parameter_key = ANY (?)
                """)) {
            statement.setArray(1, connection.createArrayOf("text", expected.keySet().toArray()));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    actual.put(result.getString("parameter_key"), result.getBigDecimal("value"));
                }
            }
        }

        assertEquals(expected.keySet(), actual.keySet());
        expected.forEach((key, value) -> assertDecimal(value, actual.get(key), key));

        ColumnMetadata parameterValue = column("scoring_parameters", "value");
        assertAll(
                () -> assertEquals("numeric", parameterValue.dataType()),
                () -> assertEquals(20, parameterValue.numericPrecision()),
                () -> assertEquals(4, parameterValue.numericScale()));
    }

    @Test
    void installsIndependentLlmDifficultyHintParameters() throws SQLException {
        Map<String, BigDecimal> actual = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT parameter_key, value
                FROM scoring_parameters
                WHERE parameter_key IN (
                    'llm_difficulty_hint_harder_delta',
                    'llm_difficulty_hint_easier_delta')
                """);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                actual.put(result.getString("parameter_key"), result.getBigDecimal("value"));
            }
        }

        assertEquals(2, actual.size());
        assertDecimal("0.20", actual.get("llm_difficulty_hint_harder_delta"));
        assertDecimal("-0.20", actual.get("llm_difficulty_hint_easier_delta"));
    }

    @Test
    void installsFeedbackSpecificConfidenceGateAndKeepsTheSharedDeltaCap() throws SQLException {
        Map<String, BigDecimal> actual = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT parameter_key, value
                FROM scoring_parameters
                WHERE parameter_key IN (
                    'llm_feedback_confidence_threshold',
                    'llm_signal_confidence_threshold',
                    'llm_signal_max_absolute_delta')
                """);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                actual.put(result.getString("parameter_key"), result.getBigDecimal("value"));
            }
        }

        assertEquals(3, actual.size());
        assertDecimal("0.70", actual.get("llm_feedback_confidence_threshold"));
        assertDecimal("0.60", actual.get("llm_signal_confidence_threshold"));
        assertDecimal("0.30", actual.get("llm_signal_max_absolute_delta"));
    }

    @Test
    void createsPlanBookkeepingColumnsFlagsAndConstraints() throws SQLException {
        Map<String, ColumnMetadata> planColumns = columns(
                "daily_plans",
                "budget_min",
                "budget_max",
                "committed_duration_minutes",
                "total_duration_minutes",
                "fallback_level");
        Map<String, ColumnMetadata> itemColumns = columns(
                "daily_plan_items", "within_budget", "repeat_notice");

        assertAll(
                () -> assertRequiredColumn(planColumns, "budget_min", "integer", "25"),
                () -> assertRequiredColumn(planColumns, "budget_max", "integer", "35"),
                () -> assertRequiredColumn(planColumns, "committed_duration_minutes", "integer", "0"),
                () -> assertRequiredColumn(planColumns, "total_duration_minutes", "integer", "0"),
                () -> assertRequiredColumn(planColumns, "fallback_level", "smallint", "0"),
                () -> assertRequiredColumn(itemColumns, "within_budget", "boolean", "true"),
                () -> assertRequiredColumn(itemColumns, "repeat_notice", "boolean", "false"),
                () -> assertNotNull(constraint("daily_plans", "chk_daily_plans_budget_range")),
                () -> assertNotNull(constraint("daily_plans", "chk_daily_plans_committed_duration")),
                () -> assertNotNull(constraint("daily_plans", "chk_daily_plans_total_duration")),
                () -> assertNotNull(constraint("daily_plans", "chk_daily_plans_fallback_level")));
    }

    @Test
    void allowsMultiplePlanRoundsForAChildOnTheSameDate() throws SQLException {
        assertNull(constraint("daily_plans", "uq_daily_plans_child_date"));
    }

    private static Map<String, BigDecimal> expectedV6Parameters() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("tiebreak_seed_a", decimal("1000003"));
        values.put("tiebreak_seed_b", decimal("10007"));
        values.put("tiebreak_seed_mod", decimal("2147483647"));
        values.put("freshness_window_divisor", decimal("6"));
        values.put("freshness_window_min", decimal("2"));
        values.put("attachment_multiplier_together", decimal("1.15"));
        values.put("attachment_multiplier_supervised", decimal("1"));
        values.put("attachment_anxiety_threshold", decimal("4"));
        values.put("attachment_exclude_independent", decimal("1"));
        values.put("attachment_guarantee_supervised", decimal("1"));
        values.put("level_max", decimal("4"));
        values.put("level_min", decimal("1"));
        values.put("level_up_threshold", decimal("3"));
        values.put("level_down_threshold", decimal("-1"));
        values.put("level_credit_stretch", decimal("1"));
        values.put("level_credit_at_level", decimal("0.5"));
        values.put("level_credit_below", decimal("0"));
        values.put("level_penalty_struggle_stretch", decimal("-0.5"));
        values.put("level_penalty_struggle_at_level", decimal("-1"));
        values.put("ceiling_counter_cap", decimal("1"));
        values.put("ceiling_sweet_spot_requires_harder", decimal("1"));
        values.put("domain_initial_level_under_48m", decimal("1"));
        values.put("domain_initial_level_48_to_60m", decimal("2"));
        values.put("domain_initial_level_60_to_73m", decimal("3"));
        return values;
    }

    private static Map<String, ColumnMetadata> columns(String table, String... names) throws SQLException {
        Map<String, ColumnMetadata> values = new LinkedHashMap<>();
        for (String name : names) {
            values.put(name, column(table, name));
        }
        return values;
    }

    private static ColumnMetadata column(String table, String name) throws SQLException {
        return queryOne("""
                SELECT data_type, is_nullable, column_default,
                       numeric_precision, numeric_scale
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """, result -> new ColumnMetadata(
                result.getString("data_type"),
                result.getString("is_nullable"),
                result.getString("column_default"),
                integer(result, "numeric_precision"),
                integer(result, "numeric_scale")), table, name);
    }

    private static boolean columnExists(String table, String name) throws SQLException {
        return queryInt("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """, table, name) == 1;
    }

    private static boolean relationExists(String name) throws SQLException {
        return queryInt("SELECT count(*) FROM pg_class WHERE oid = to_regclass(?)", name) == 1;
    }

    private static String constraint(String table, String name) throws SQLException {
        return queryOne("""
                SELECT pg_get_constraintdef(oid) AS definition
                FROM pg_constraint
                WHERE conrelid = ?::regclass
                  AND conname = ?
                """, result -> result.getString("definition"), table, name);
    }

    private static void assertRequiredColumn(
            Map<String, ColumnMetadata> columns, String name, String type, String defaultPrefix) {
        ColumnMetadata column = columns.get(name);
        assertNotNull(column, name);
        assertEquals(type, column.dataType(), name + " type");
        assertEquals("NO", column.nullable(), name + " nullability");
        assertNotNull(column.columnDefault(), name + " default");
        assertTrue(column.columnDefault().startsWith(defaultPrefix), name + " default");
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertDecimal(decimal(expected), actual, null);
    }

    private static void assertDecimal(BigDecimal expected, BigDecimal actual, String message) {
        assertNotNull(actual, message);
        assertEquals(0, expected.compareTo(actual), message);
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static Integer integer(ResultSet result, String column) throws SQLException {
        int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }

    private static int queryInt(String sql, Object... parameters) throws SQLException {
        return queryOne(sql, result -> result.getInt(1), parameters);
    }

    private static long insertReturningLong(String sql, Object... parameters) throws SQLException {
        return queryOne(sql, result -> result.getLong(1), parameters);
    }

    private static int executeUpdate(String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            return statement.executeUpdate();
        }
    }

    private static void assertPublicationRejected(long activityId, String expectedMessage) {
        SQLException exception = assertThrows(SQLException.class,
                () -> executeUpdate("UPDATE activities SET status = 'PUBLISHED' WHERE id = ?", activityId));
        assertAll(
                () -> assertEquals("23514", exception.getSQLState()),
                () -> assertTrue(exception.getMessage().contains(expectedMessage), exception.getMessage()));
    }

    private static <T> T queryOne(String sql, RowMapper<T> mapper, Object... parameters)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next(), "Query returned no rows: " + sql);
                T value = mapper.map(result);
                assertFalse(result.next(), "Query returned multiple rows: " + sql);
                return value;
            }
        }
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet result) throws SQLException;
    }

    private record MigrationHistory(
            int migrationCount, int firstVersion, int lastVersion, boolean allSuccessful) {
    }

    private record ActivityPool(
            int activityCount,
            long firstId,
            long lastId,
            int activeHomeCount,
            int distinctIdCount) {
    }

    private record DunnProfile(
            int noiseTolerance,
            int visualTolerance,
            int movementTolerance,
            BigDecimal noiseWeight,
            BigDecimal visualWeight,
            BigDecimal movementWeight) {
    }

    private record ColumnMetadata(
            String dataType,
            String nullable,
            String columnDefault,
            Integer numericPrecision,
            Integer numericScale) {
    }
}
