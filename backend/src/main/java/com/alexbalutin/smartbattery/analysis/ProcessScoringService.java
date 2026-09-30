
package com.alexbalutin.smartbattery.analysis;

import com.alexbalutin.smartbattery.model.ProcessDrainScore;
import com.alexbalutin.smartbattery.model.ProcessSample;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProcessScoringService {

    private static final double CPU_WEIGHT = 0.50;
    private static final double CPU_SPIKE_WEIGHT = 0.25;
    private static final double MEMORY_WEIGHT = 0.10;
    private static final double PRESENCE_WEIGHT = 0.15;

    private static final double CPU_NORMALIZATION_PERCENT = 40.0;
    private static final double CPU_SPIKE_NORMALIZATION_PERCENT = 25.0;
    private static final double MEMORY_NORMALIZATION_BYTES = 2.0 * 1024 * 1024 * 1024;

    public List<ProcessDrainScore> score(
            long intervalId,
            List<ProcessSample> intervalSamples,
            List<ProcessSample> baselineSamples,
            int limit) {

        if (intervalSamples == null || intervalSamples.isEmpty()) {
            return List.of();
        }

        Map<String, Double> baselineCpuByProcess = averageCpuByProcess(baselineSamples);
        Map<String, List<ProcessSample>> grouped = intervalSamples.stream()
                .collect(Collectors.groupingBy(
                        sample -> normalizeName(sample.processName()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        long totalTimestamps = intervalSamples.stream()
                .map(ProcessSample::timestamp)
                .distinct()
                .count();

        return grouped.entrySet().stream()
                .map(entry -> toScore(
                        intervalId,
                        entry.getValue(),
                        baselineCpuByProcess.getOrDefault(entry.getKey(), 0.0),
                        totalTimestamps))
                .sorted(Comparator.comparingDouble(ProcessDrainScore::score).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    private ProcessDrainScore toScore(
            long intervalId,
            List<ProcessSample> samples,
            double baselineCpu,
            long totalTimestamps) {

        double averageCpu = samples.stream()
                .mapToDouble(ProcessSample::cpuUsagePercent)
                .average()
                .orElse(0.0);

        double averageMemory = samples.stream()
                .mapToLong(ProcessSample::memoryBytes)
                .average()
                .orElse(0.0);

        double cpuIncrease = Math.max(0.0, averageCpu - baselineCpu);

        long presentTimestamps = samples.stream()
                .map(ProcessSample::timestamp)
                .distinct()
                .count();
        double presenceRatio = totalTimestamps == 0
                ? 0.0
                : (double) presentTimestamps / totalTimestamps;

        double normalizedCpu = clamp01(averageCpu / CPU_NORMALIZATION_PERCENT);
        double normalizedSpike = clamp01(cpuIncrease / CPU_SPIKE_NORMALIZATION_PERCENT);
        double normalizedMemory = clamp01(averageMemory / MEMORY_NORMALIZATION_BYTES);

        double raw = CPU_WEIGHT * normalizedCpu
                + CPU_SPIKE_WEIGHT * normalizedSpike
                + MEMORY_WEIGHT * normalizedMemory
                + PRESENCE_WEIGHT * clamp01(presenceRatio);

        String displayName = samples.get(0).processName();
        return new ProcessDrainScore(
                0,
                intervalId,
                displayName,
                round(raw * 100.0, 2),
                round(averageCpu, 2),
                round(baselineCpu, 2),
                round(cpuIncrease, 2),
                Math.round(averageMemory),
                round(presenceRatio, 4),
                samples.size());
    }

    private Map<String, Double> averageCpuByProcess(List<ProcessSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return Map.of();
        }

        Map<String, double[]> accumulators = new HashMap<>();
        for (ProcessSample sample : samples) {
            double[] values = accumulators.computeIfAbsent(
                    normalizeName(sample.processName()),
                    ignored -> new double[2]);
            values[0] += sample.cpuUsagePercent();
            values[1] += 1.0;
        }

        Map<String, Double> result = new HashMap<>();
        accumulators.forEach((name, values) -> result.put(name, values[0] / values[1]));
        return result;
    }

    private static String normalizeName(String name) {
        return name == null ? "unknown" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
