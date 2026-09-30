
package com.alexbalutin.smartbattery.analysis;

import com.alexbalutin.smartbattery.model.BatterySample;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

@Component
public class BatteryDrainCalculator {

    public OptionalDouble calculatePercentPerHour(List<BatterySample> samples) {
        if (samples == null || samples.size() < 2) {
            return OptionalDouble.empty();
        }

        BatterySample first = samples.get(0);
        BatterySample last = samples.get(samples.size() - 1);

        if (first.batteryPercent() == null || last.batteryPercent() == null) {
            return OptionalDouble.empty();
        }

        for (BatterySample sample : samples) {
            if (Boolean.TRUE.equals(sample.charging()) || !Boolean.TRUE.equals(sample.discharging())) {
                return OptionalDouble.empty();
            }
        }

        long durationMillis = Duration.between(first.timestamp(), last.timestamp()).toMillis();
        if (durationMillis <= 0) {
            return OptionalDouble.empty();
        }

        double percentageLost = first.batteryPercent() - last.batteryPercent();
        if (percentageLost < 0.0) {
            return OptionalDouble.empty();
        }

        double durationHours = durationMillis / 3_600_000.0;
        return OptionalDouble.of(percentageLost / durationHours);
    }

    public List<Double> calculateNonOverlappingWindowRates(
            List<BatterySample> samples,
            Duration window) {

        List<Double> rates = new ArrayList<>();
        if (samples == null || samples.size() < 2 || window.isZero() || window.isNegative()) {
            return rates;
        }

        int startIndex = 0;
        while (startIndex < samples.size() - 1) {
            BatterySample start = samples.get(startIndex);
            long targetMillis = start.timestamp().plus(window).toEpochMilli();

            int endIndex = startIndex + 1;
            while (endIndex < samples.size()
                    && samples.get(endIndex).timestamp().toEpochMilli() < targetMillis) {
                endIndex++;
            }

            if (endIndex >= samples.size()) {
                break;
            }

            OptionalDouble rate = calculatePercentPerHour(samples.subList(startIndex, endIndex + 1));
            rate.ifPresent(rates::add);
            startIndex = endIndex;
        }

        return rates;
    }
}
