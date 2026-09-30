
package com.alexbalutin.smartbattery.analysis;

import com.alexbalutin.smartbattery.model.BatterySample;
import com.alexbalutin.smartbattery.model.DrainInterval;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TelemetryAggregationServiceTest {

    @Test
    void summarizesWindowedDrainAndEventCount() {
        BatteryDrainCalculator calculator = new BatteryDrainCalculator();
        TelemetryAggregationService service = new TelemetryAggregationService(calculator);
        Instant start = Instant.parse("2026-09-29T12:00:00Z");

        List<BatterySample> samples = List.of(
                sample(start, 80.0),
                sample(start.plusSeconds(300), 79.5),
                sample(start.plusSeconds(600), 79.0));

        List<DrainInterval> intervals = List.of(
                new DrainInterval(1, start, start.plusSeconds(600),
                        80.0, 79.0, 6.0, "MODERATE", 4.0, 0.5));

        TelemetryAggregationService.SummaryStats result = service.summarize(
                samples,
                intervals,
                Duration.ofMinutes(5));

        assertNotNull(result.averageDrainRatePercentPerHour());
        assertEquals(1, result.highDrainEventCount());
    }

    private BatterySample sample(Instant time, double percent) {
        return new BatterySample(1, time, percent, null, null, 11.7,
                9.0, false, true, false, "test", "mock");
    }
}
