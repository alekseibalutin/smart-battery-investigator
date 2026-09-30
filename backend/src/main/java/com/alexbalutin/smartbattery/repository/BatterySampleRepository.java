
package com.alexbalutin.smartbattery.repository;

import com.alexbalutin.smartbattery.model.BatterySample;
import com.alexbalutin.smartbattery.telemetry.BatteryReading;
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
public class BatterySampleRepository {

    private final JdbcTemplate jdbcTemplate;

    public BatterySampleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BatterySample insert(Instant timestamp, BatteryReading reading, String telemetryMode) {
        String sql = """
                INSERT INTO battery_sample (
                    sampled_at, battery_percent, remaining_capacity, capacity_units,
                    voltage_volts, discharge_rate_watts, charging, discharging,
                    power_online, source_name, telemetry_mode
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, timestamp.toEpochMilli());
            setNullableDouble(statement, 2, reading.batteryPercent());
            setNullableInteger(statement, 3, reading.remainingCapacity());
            statement.setString(4, reading.capacityUnits());
            setNullableDouble(statement, 5, reading.voltageVolts());
            setNullableDouble(statement, 6, reading.dischargeRateWatts());
            setNullableBoolean(statement, 7, reading.charging());
            setNullableBoolean(statement, 8, reading.discharging());
            setNullableBoolean(statement, 9, reading.powerOnline());
            statement.setString(10, reading.sourceName());
            statement.setString(11, telemetryMode);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("SQLite did not return a generated battery sample ID");
        }

        return new BatterySample(
                key.longValue(),
                timestamp,
                reading.batteryPercent(),
                reading.remainingCapacity(),
                reading.capacityUnits(),
                reading.voltageVolts(),
                reading.dischargeRateWatts(),
                reading.charging(),
                reading.discharging(),
                reading.powerOnline(),
                reading.sourceName(),
                telemetryMode);
    }

    public Optional<BatterySample> findLatest() {
        List<BatterySample> results = jdbcTemplate.query(
                "SELECT * FROM battery_sample ORDER BY sampled_at DESC LIMIT 1",
                (rs, rowNum) -> mapRow(rs));
        return results.stream().findFirst();
    }

    public List<BatterySample> findSince(Instant since) {
        return jdbcTemplate.query(
                "SELECT * FROM battery_sample WHERE sampled_at >= ? ORDER BY sampled_at ASC",
                (rs, rowNum) -> mapRow(rs),
                since.toEpochMilli());
    }

    public List<BatterySample> findBetween(Instant start, Instant end) {
        return jdbcTemplate.query(
                "SELECT * FROM battery_sample WHERE sampled_at BETWEEN ? AND ? ORDER BY sampled_at ASC",
                (rs, rowNum) -> mapRow(rs),
                start.toEpochMilli(),
                end.toEpochMilli());
    }

    public long count() {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM battery_sample", Long.class);
        return value == null ? 0L : value;
    }

    public boolean ping() {
        Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return value != null && value == 1;
    }

    private BatterySample mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new BatterySample(
                rs.getLong("id"),
                Instant.ofEpochMilli(rs.getLong("sampled_at")),
                nullableDouble(rs, "battery_percent"),
                nullableInteger(rs, "remaining_capacity"),
                rs.getString("capacity_units"),
                nullableDouble(rs, "voltage_volts"),
                nullableDouble(rs, "discharge_rate_watts"),
                nullableBoolean(rs, "charging"),
                nullableBoolean(rs, "discharging"),
                nullableBoolean(rs, "power_online"),
                rs.getString("source_name"),
                rs.getString("telemetry_mode"));
    }

    private static void setNullableDouble(PreparedStatement statement, int index, Double value)
            throws java.sql.SQLException {
        if (value == null) {
            statement.setNull(index, Types.REAL);
        } else {
            statement.setDouble(index, value);
        }
    }

    private static void setNullableInteger(PreparedStatement statement, int index, Integer value)
            throws java.sql.SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static void setNullableBoolean(PreparedStatement statement, int index, Boolean value)
            throws java.sql.SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value ? 1 : 0);
        }
    }

    private static Double nullableDouble(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer nullableInteger(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Boolean nullableBoolean(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value != 0;
    }
}
