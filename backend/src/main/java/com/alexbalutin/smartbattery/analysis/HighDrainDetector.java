
package com.alexbalutin.smartbattery.analysis;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HighDrainDetector {

    public Decision evaluate(
            double currentRate,
            List<Double> historicalRates,
            double minimumThreshold,
            double standardDeviationMultiplier,
            int minimumBaselineWindows) {

        List<Double> finiteRates = historicalRates == null
                ? List.of()
                : historicalRates.stream().filter(Double::isFinite).toList();

        double mean = finiteRates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = finiteRates.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
        double standardDeviation = Math.sqrt(variance);

        boolean aboveAbsoluteThreshold = currentRate >= minimumThreshold;
        boolean enoughHistory = finiteRates.size() >= minimumBaselineWindows;
        double statisticalThreshold = mean + standardDeviationMultiplier * standardDeviation;

        boolean highDrain = aboveAbsoluteThreshold
                && (!enoughHistory || currentRate > statisticalThreshold);

        return new Decision(
                highDrain,
                severity(currentRate, minimumThreshold),
                enoughHistory ? mean : null,
                enoughHistory ? standardDeviation : null,
                enoughHistory ? statisticalThreshold : null,
                finiteRates.size());
    }

    private String severity(double currentRate, double minimumThreshold) {
        if (currentRate >= minimumThreshold * 2.5) {
            return "CRITICAL";
        }
        if (currentRate >= minimumThreshold * 1.75) {
            return "HIGH";
        }
        return "MODERATE";
    }

    public record Decision(
            boolean highDrain,
            String severity,
            Double baselineMean,
            Double baselineStdDev,
            Double statisticalThreshold,
            int baselineWindowCount) {
    }
}
