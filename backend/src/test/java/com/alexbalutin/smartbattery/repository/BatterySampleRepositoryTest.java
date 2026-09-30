
package com.alexbalutin.smartbattery.repository;

import com.alexbalutin.smartbattery.telemetry.BatteryReading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatterySampleRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void insertsAndReadsLatestSample() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("repository-test.db"));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE battery_sample (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sampled_at INTEGER NOT NULL,
                    battery_percent REAL,
                    remaining_capacity INTEGER,
                    capacity_units TEXT,
                    voltage_volts REAL,
                    discharge_rate_watts REAL,
                    charging INTEGER,
                    discharging INTEGER,
                    power_online INTEGER,
                    source_name TEXT,
                    telemetry_mode TEXT NOT NULL
                )
                """);

        BatterySampleRepository repository = new BatterySampleRepository(jdbcTemplate);
        Instant timestamp = Instant.parse("2026-09-29T12:00:00Z");

        repository.insert(timestamp, new BatteryReading(
                72.5, 36000, "MWH", 11.8, 12.4,
                false, true, false, "Test Battery"), "mock");

        assertEquals(1, repository.count());
        assertTrue(repository.findLatest().isPresent());
        assertEquals(72.5, repository.findLatest().orElseThrow().batteryPercent(), 0.0001);
    }
}
