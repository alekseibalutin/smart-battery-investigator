
package com.alexbalutin.smartbattery.api.dto;

import java.time.Instant;
import java.util.List;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record BatterySampleDto(
            long id,
            Instant timestamp,
            Double batteryPercent,
            Integer remainingCapacity,
            String capacityUnits,
            Double voltageVolts,
            Double dischargeRateWatts,
            Boolean charging,
            Boolean discharging,
            Boolean powerOnline,
            String sourceName,
            String telemetryMode) {
    }

    public record ProcessSampleDto(
            int pid,
            String processName,
            double cpuUsagePercent,
            long memoryBytes,
            String executablePath,
            Long processStartTimeEpochMs,
            long cumulativeCpuMs) {
    }

    public record DrainIntervalDto(
            long id,
            Instant startTime,
            Instant endTime,
            long durationSeconds,
            double startingBatteryPercent,
            double endingBatteryPercent,
            double batteryPercentLost,
            double calculatedDrainRatePercentPerHour,
            String severity,
            Double baselineMean,
            Double baselineStdDev) {
    }

    public record ProcessScoreDto(
            String processName,
            double score,
            double averageCpuUsage,
            double baselineCpuUsage,
            double cpuIncreaseAboveBaseline,
            double averageMemoryBytes,
            double presenceRatio,
            int sampleCount) {
    }

    public record DrainIntervalDetailDto(
            DrainIntervalDto interval,
            List<BatterySampleDto> batteryTimeline,
            List<ProcessScoreDto> associatedProcesses) {
    }

    public record ProcessRankingDto(
            String processName,
            double averageScore,
            long highDrainIntervalCount,
            double averageCpuUsage,
            double averageMemoryBytes,
            long sampleCount) {
    }

    public record AnalyticsSummaryDto(
            int hours,
            Double averageDrainRatePercentPerHour,
            Double maximumDrainRatePercentPerHour,
            int highDrainEventCount,
            List<ProcessRankingDto> mostFrequentlyAssociatedProcesses) {
    }

    public record StatusDto(
            boolean telemetryCollectionActive,
            String telemetryMode,
            BatterySampleDto currentBattery,
            Double currentEstimatedDrainRatePercentPerHour,
            String databaseStatus,
            long storedBatterySamples,
            long samplingIntervalMs,
            Instant lastSuccessfulCollection,
            String lastCollectionError) {
    }

    public record ErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path) {
    }
}
