
package com.alexbalutin.smartbattery.api.dto;

import com.alexbalutin.smartbattery.model.BatterySample;
import com.alexbalutin.smartbattery.model.DrainInterval;
import com.alexbalutin.smartbattery.model.ProcessDrainScore;
import com.alexbalutin.smartbattery.model.ProcessRankingAggregate;
import com.alexbalutin.smartbattery.model.ProcessSample;

public final class ApiMapper {

    private ApiMapper() {
    }

    public static ApiDtos.BatterySampleDto battery(BatterySample sample) {
        return new ApiDtos.BatterySampleDto(
                sample.id(),
                sample.timestamp(),
                sample.batteryPercent(),
                sample.remainingCapacity(),
                sample.capacityUnits(),
                sample.voltageVolts(),
                sample.dischargeRateWatts(),
                sample.charging(),
                sample.discharging(),
                sample.powerOnline(),
                sample.sourceName(),
                sample.telemetryMode());
    }

    public static ApiDtos.ProcessSampleDto process(ProcessSample sample) {
        return new ApiDtos.ProcessSampleDto(
                sample.pid(),
                sample.processName(),
                sample.cpuUsagePercent(),
                sample.memoryBytes(),
                sample.executablePath(),
                sample.processStartTimeEpochMs(),
                sample.cumulativeCpuMs());
    }

    public static ApiDtos.DrainIntervalDto interval(DrainInterval interval) {
        return new ApiDtos.DrainIntervalDto(
                interval.id(),
                interval.startTime(),
                interval.endTime(),
                interval.durationSeconds(),
                interval.startingBatteryPercent(),
                interval.endingBatteryPercent(),
                interval.batteryPercentLost(),
                interval.calculatedDrainRate(),
                interval.severity(),
                interval.baselineMean(),
                interval.baselineStdDev());
    }

    public static ApiDtos.ProcessScoreDto score(ProcessDrainScore score) {
        return new ApiDtos.ProcessScoreDto(
                score.processName(),
                score.score(),
                score.averageCpuUsage(),
                score.baselineCpuUsage(),
                score.cpuIncreaseAboveBaseline(),
                score.averageMemoryBytes(),
                score.presenceRatio(),
                score.sampleCount());
    }

    public static ApiDtos.ProcessRankingDto ranking(ProcessRankingAggregate ranking) {
        return new ApiDtos.ProcessRankingDto(
                ranking.processName(),
                ranking.averageScore(),
                ranking.intervalCount(),
                ranking.averageCpuUsage(),
                ranking.averageMemoryBytes(),
                ranking.sampleCount());
    }
}
