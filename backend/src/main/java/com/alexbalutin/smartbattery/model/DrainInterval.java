
package com.alexbalutin.smartbattery.model;

import java.time.Instant;

public record DrainInterval(
        long id,
        Instant startTime,
        Instant endTime,
        double startingBatteryPercent,
        double endingBatteryPercent,
        double calculatedDrainRate,
        String severity,
        Double baselineMean,
        Double baselineStdDev) {

    public double batteryPercentLost() {
        return Math.max(0.0, startingBatteryPercent - endingBatteryPercent);
    }

    public long durationSeconds() {
        return Math.max(0L, endTime.getEpochSecond() - startTime.getEpochSecond());
    }
}
