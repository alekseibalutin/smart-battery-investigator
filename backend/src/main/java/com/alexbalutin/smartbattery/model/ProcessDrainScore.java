
package com.alexbalutin.smartbattery.model;

public record ProcessDrainScore(
        long id,
        long intervalId,
        String processName,
        double score,
        double averageCpuUsage,
        double baselineCpuUsage,
        double cpuIncreaseAboveBaseline,
        double averageMemoryBytes,
        double presenceRatio,
        int sampleCount) {
}
