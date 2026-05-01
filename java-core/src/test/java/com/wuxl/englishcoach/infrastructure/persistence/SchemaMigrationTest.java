package com.wuxl.englishcoach.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class SchemaMigrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:schema-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.url", () -> "jdbc:h2:mem:schema-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.flyway.user", () -> "sa");
        registry.add("spring.flyway.password", () -> "");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allTenTablesShouldExist() {
        String[] tables = {
                "user_profile", "learning_item", "mastery_state",
                "study_session", "attempt_log", "daily_plan_snapshot",
                "daily_plan_item", "weekly_review_snapshot"
        };

        for (String table : tables) {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where upper(table_name) = upper(?)",
                    Integer.class, table
            );
            assertThat(count).as("Table %s should exist", table).isEqualTo(1);
        }
    }

    @Test
    void userProfileShouldHaveLevelColumns() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where upper(table_name) = 'USER_PROFILE' and upper(column_name) = 'OVERALL_LEVEL'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void seedLearningItemsShouldBeLoaded() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from learning_item", Integer.class
        );
        assertThat(count).isGreaterThanOrEqualTo(20);
    }

    @Test
    void learningItemShouldHaveDifficultyColumn() {
        Integer colCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where upper(table_name) = 'LEARNING_ITEM' and upper(column_name) = 'DIFFICULTY'",
                Integer.class
        );
        assertThat(colCount).isEqualTo(1);
    }

    @Test
    void masteryStateShouldHaveUniqueUserItemConstraint() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.indexes where upper(table_name) = 'MASTERY_STATE' and upper(index_name) like '%USER_ITEM%'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }
}
