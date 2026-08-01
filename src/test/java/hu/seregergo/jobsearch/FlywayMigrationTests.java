package hu.seregergo.jobsearch;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlywayMigrationTests extends PostgreSqlIntegrationTest {

    @Test
    void upgradesExistingV1RowsAndAddsTheCompleteApplicationSchema() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        );
        Flyway v1 = Flyway.configure()
            .dataSource(dataSource)
            .cleanDisabled(false)
            .target("1")
            .load();
        v1.clean();
        v1.migrate();

        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-30T12:30:00Z");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
            """
                INSERT INTO job_postings (
                    id,
                    company_name,
                    role_title,
                    source,
                    source_url,
                    work_mode,
                    found_on,
                    classification,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            id,
            "Existing Company",
            "Existing Java role",
            "Company careers",
            "https://example.com/jobs/existing",
            "HYBRID",
            Date.valueOf(LocalDate.of(2026, 7, 29)),
            "A",
            Timestamp.from(createdAt)
        );

        Flyway.configure().dataSource(dataSource).load().migrate();

        Map<String, Object> migrated = jdbcTemplate.queryForMap(
            """
                SELECT target_track, description_snapshot, created_at, updated_at
                FROM job_postings
                WHERE id = ?
                """,
            id
        );
        assertEquals("JAVA", migrated.get("target_track"));
        assertNull(migrated.get("description_snapshot"));
        assertEquals(migrated.get("created_at"), migrated.get("updated_at"));
        assertEquals(
            "applications",
            jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.applications')::text",
                String.class
            )
        );
        assertEquals(
            "submitted_cvs",
            jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.submitted_cvs')::text",
                String.class
            )
        );
        assertEquals(
            "application_idempotency_records",
            jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.application_idempotency_records')::text",
                String.class
            )
        );
        assertEquals(
            "4",
            jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history "
                    + "WHERE success ORDER BY installed_rank DESC LIMIT 1",
                String.class
            )
        );
    }
}
