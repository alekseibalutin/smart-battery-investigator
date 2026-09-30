
package com.alexbalutin.smartbattery.repository;

import com.alexbalutin.smartbattery.model.ProcessSample;
import com.alexbalutin.smartbattery.telemetry.ProcessReading;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.Instant;
import java.util.List;

@Repository
public class ProcessSampleRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProcessSampleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertBatch(long batterySampleId, Instant timestamp, List<ProcessReading> readings) {
        if (readings.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO process_sample (
                    battery_sample_id, sampled_at, pid, process_name, cpu_usage_percent,
                    memory_bytes, executable_path, process_start_time, cumulative_cpu_ms
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, readings, 100, (statement, reading) -> {
            statement.setLong(1, batterySampleId);
            statement.setLong(2, timestamp.toEpochMilli());
            statement.setInt(3, reading.pid());
            statement.setString(4, reading.processName());
            statement.setDouble(5, reading.cpuUsagePercent());
            statement.setLong(6, reading.memoryBytes());
            statement.setString(7, reading.executablePath());
            if (reading.processStartTimeEpochMs() == null) {
                statement.setNull(8, Types.BIGINT);
            } else {
                statement.setLong(8, reading.processStartTimeEpochMs());
            }
            statement.setLong(9, reading.cumulativeCpuMs());
        });
    }

    public List<ProcessSample> findLatestSnapshot() {
        return jdbcTemplate.query("""
                        SELECT ps.*
                        FROM process_sample ps
                        WHERE ps.battery_sample_id = (
                            SELECT id FROM battery_sample ORDER BY sampled_at DESC LIMIT 1
                        )
                        ORDER BY ps.cpu_usage_percent DESC, ps.memory_bytes DESC
                        """,
                (rs, rowNum) -> mapRow(rs));
    }

    public List<ProcessSample> findBetween(Instant start, Instant end) {
        return jdbcTemplate.query(
                "SELECT * FROM process_sample WHERE sampled_at BETWEEN ? AND ? ORDER BY sampled_at ASC",
                (rs, rowNum) -> mapRow(rs),
                start.toEpochMilli(),
                end.toEpochMilli());
    }

    private ProcessSample mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        long startTime = rs.getLong("process_start_time");
        Long nullableStart = rs.wasNull() ? null : startTime;

        return new ProcessSample(
                rs.getLong("id"),
                rs.getLong("battery_sample_id"),
                Instant.ofEpochMilli(rs.getLong("sampled_at")),
                rs.getInt("pid"),
                rs.getString("process_name"),
                rs.getDouble("cpu_usage_percent"),
                rs.getLong("memory_bytes"),
                rs.getString("executable_path"),
                nullableStart,
                rs.getLong("cumulative_cpu_ms"));
    }
}
