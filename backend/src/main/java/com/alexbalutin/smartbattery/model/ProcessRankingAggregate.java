
package com.alexbalutin.smartbattery.model;

public record ProcessRankingAggregate(
        String processName,
        double averageScore,
        long intervalCount,
        double averageCpuUsage,
        double averageMemoryBytes,
        long sampleCount) {
}
