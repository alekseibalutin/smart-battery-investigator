
package com.alexbalutin.smartbattery.analysis;

import com.alexbalutin.smartbattery.model.BatterySample;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatteryDrainCalculatorTest {

    private final BatteryDrainCalculator calculator = new BatteryDrainCalculator();

    @Test
    void calculatesNormalizedDrainRateAcrossWindow() {
        Instant start = Instant.parse("2026-09-29T12:00:00Z");
        List<BatterySample> samples = List.of(
                sample(start, 80.0, false, true),
                sample(start.plusSeconds(300), 79.0, false, true),
                sample(start.plusSeconds(600), 78.0, false, true));

        OptionalDouble result = calculator.calculatePercentPerHour(samples);

        assertTrue(result.isPresent());
        assertEquals(12.0, result.getAsDouble(), 0.0001);
    }

    @Test
    void excludesChargingWindows() {
        Instant start = Instant.parse("2026-09-29T12:00:00Z");
        List<BatterySample> samples = List.of(
                sample(start, 80.0, false, true),
                sample(start.plusSeconds(300), 80.5, true, false),
                sample(start.plusSeconds(600), 81.0, true, false));

        assertTrue(calculator.calculatePercentPerHour(samples).isEmpty());
    }

    @Test
    void excludesWindowsWithUnknownDischargeState() {
        Instant start = Instant.parse("2026-09-29T12:00:00Z");
        List<BatterySample> samples = List.of(
                sample(start, 80.0, false, true),
                new BatterySample(2, start.plusSeconds(600), 79.0, null, null, null,
                        null, false, null, false, "test", "mock"));

        assertTrue(calculator.calculatePercentPerHour(samples).isEmpty());
    }

    private BatterySample sample(Instant time, double percent, Boolean charging, Boolean discharging) {
        return new BatterySample(
                1,
                time,
                percent,
                null,
                null,
                11.7,
                10.0,
                charging,
                discharging,
                false,
                "test",
                "mock");
    }
}
