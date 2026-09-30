
package com.alexbalutin.smartbattery.analysis;

import com.alexbalutin.smartbattery.model.BatterySample;
import com.alexbalutin.smartbattery.model.DrainInterval;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class TelemetryAggregationService {

    private final BatteryDrainCalculator drainCalculator;

    public TelemetryAggregationService(BatteryDrainCalculator drainCalculator) {
        this.drainCalculator = drainCalculator;
    }

    public SummaryStats summarize(
            List<BatterySample> batterySamples,
            List<DrainInterval> intervals,
            Duration analysisWindow) {

        List<Double> rates = drainCalculator.calculateNonOverlappingWindowRates(
                batterySamples,
                analysisWindow);

        Double averageRate = rates.isEmpty()
                ? null
                : rates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        Double maximumRate = rates.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null);

        if (intervals != null && !intervals.isEmpty()) {
            double intervalMaximum = intervals.stream()
                    .mapToDouble(DrainInterval::calculatedDrainRate)
                    .max()
                    .orElse(0.0);
            maximumRate = maximumRate == null ? intervalMaximum : Math.max(maximumRate, intervalMaximum);
        }

        return new SummaryStats(
                roundNullable(averageRate),
                roundNullable(maximumRate),
                intervals == null ? 0 : intervals.size());
    }

    private static Double roundNullable(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    public record SummaryStats(
            Double averageDrainRatePercentPerHour,
            Double maximumDrainRatePercentPerHour,
            int highDrainEventCount) {
    }
}
