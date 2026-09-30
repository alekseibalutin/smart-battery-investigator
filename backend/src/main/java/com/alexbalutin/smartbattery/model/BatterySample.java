
package com.alexbalutin.smartbattery.model;

import java.time.Instant;

public record BatterySample(
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
