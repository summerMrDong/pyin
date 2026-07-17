package com.pyin.plugin.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:core-schema-init;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class CoreSchemaInitializationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldInitializeCoreAndBusinessTablesOnStartup() {
        Integer pluginTableCount = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name = 'pyin_plugin'
                """, Integer.class);
        Integer userTableCount = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name = 'pyin_user'
                """, Integer.class);
        Integer statusColumnCount = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = 'public' and table_name = 'pyin_user' and column_name = 'status'
                """, Integer.class);
        Integer pluginRows = jdbcTemplate.queryForObject("select count(*) from pyin_plugin", Integer.class);

        assertThat(pluginTableCount).isEqualTo(1);
        assertThat(userTableCount).isEqualTo(1);
        assertThat(statusColumnCount).isEqualTo(1);
        assertThat(pluginRows).isNotNull().isGreaterThan(0);
    }
}
