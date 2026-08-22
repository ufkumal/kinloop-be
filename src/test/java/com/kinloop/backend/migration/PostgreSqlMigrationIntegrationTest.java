package com.kinloop.backend.migration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void appliesEveryMigrationThroughV21() throws SQLException {
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
                () -> assertEquals(21, migrationsExecuted),
                () -> assertEquals(21, history.migrationCount()),
                () -> assertEquals(1, history.firstVersion()),
                () -> assertEquals(21, history.lastVersion()),
                () -> assertTrue(history.allSuccessful()));
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
        int activitiesWithOutcomes = queryInt("SELECT count(DISTINCT activity_id) FROM activity_outcomes");

        assertAll(
                () -> assertEquals(243, pool.activityCount()),
                () -> assertEquals(1L, pool.firstId()),
                () -> assertEquals(243L, pool.lastId()),
                () -> assertEquals(243, pool.activeHomeCount()),
                () -> assertEquals(243, pool.distinctIdCount()),
                () -> assertEquals(243, instructionCount),
                () -> assertEquals(243, activitiesWithAtLeastFourSteps),
                () -> assertEquals(243, activitiesWithOutcomes),
                () -> assertFalse(columnExists("activities", "is_scaffolded")),
                () -> assertFalse(relationExists("v_scorable_activities")));
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
