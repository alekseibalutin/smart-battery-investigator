
package com.alexbalutin.smartbattery.repository;

import com.alexbalutin.smartbattery.model.DrainInterval;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class DrainIntervalRepository {

    private final JdbcTemplate jdbcTemplate;

    public DrainIntervalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DrainInterval insert(
            Instant startTime,
            Instant endTime,
            double startingPercent,
            double endingPercent,
            double drainRate,
            String severity,
            Double baselineMean,
            Double baselineStdDev) {

        String sql = """
                INSERT INTO drain_interval (
                    start_time, end_time, starting_battery_percent, ending_battery_percent,
                    calculated_drain_rate, severity, baseline_mean, baseline_stddev
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, startTime.toEpochMilli());
            statement.setLong(2, endTime.toEpochMilli());
            statement.setDouble(3, startingPercent);
            statement.setDouble(4, endingPercent);
            statement.setDouble(5, drainRate);
            statement.setString(6, severity);
            setNullableDouble(statement, 7, baselineMean);
            setNullableDouble(statement, 8, baselineStdDev);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("SQLite did not return a generated drain interval ID");
        }

        return new DrainInterval(
                key.longValue(),
                startTime,
                endTime,
                startingPercent,
                endingPercent,
                drainRate,
                severity,
                baselineMean,
                baselineStdDev);
    }

    public DrainInterval update(DrainInterval interval) {
        jdbcTemplate.update("""
                        UPDATE drain_interval
                        SET start_time = ?, end_time = ?, starting_battery_percent = ?,
                            ending_battery_percent = ?, calculated_drain_rate = ?, severity = ?,
                            baseline_mean = ?, baseline_stddev = ?
                        WHERE id = ?
                        """,
                interval.startTime().toEpochMilli(),
                interval.endTime().toEpochMilli(),
                interval.startingBatteryPercent(),
                interval.endingBatteryPercent(),
                interval.calculatedDrainRate(),
                interval.severity(),
                interval.baselineMean(),
                interval.baselineStdDev(),
                interval.id());
        return interval;
    }

    public Optional<DrainInterval> findById(long id) {
        List<DrainInterval> results = jdbcTemplate.query(
                "SELECT * FROM drain_interval WHERE id = ?",
                (rs, rowNum) -> mapRow(rs),
                id);
        return results.stream().findFirst();
    }

    public Optional<DrainInterval> findMostRecent() {
        List<DrainInterval> results = jdbcTemplate.query(
                "SELECT * FROM drain_interval ORDER BY end_time DESC LIMIT 1",
                (rs, rowNum) -> mapRow(rs));
        return results.stream().findFirst();
    }

    public List<DrainInterval> findSince(Instant since) {
        return jdbcTemplate.query(
                "SELECT * FROM drain_interval WHERE end_time >= ? ORDER BY start_time DESC",
                (rs, rowNum) -> mapRow(rs),
                since.toEpochMilli());
    }

    private DrainInterval mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new DrainInterval(
                rs.getLong("id"),
                Instant.ofEpochMilli(rs.getLong("start_time")),
                Instant.ofEpochMilli(rs.getLong("end_time")),
                rs.getDouble("starting_battery_percent"),
                rs.getDouble("ending_battery_percent"),
                rs.getDouble("calculated_drain_rate"),
                rs.getString("severity"),
                nullableDouble(rs, "baseline_mean"),
                nullableDouble(rs, "baseline_stddev"));
    }

    private static void setNullableDouble(PreparedStatement statement, int index, Double value)
            throws java.sql.SQLException {
        if (value == null) {
            statement.setNull(index, Types.REAL);
        } else {
            statement.setDouble(index, value);
        }
    }

    private static Double nullableDouble(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}
