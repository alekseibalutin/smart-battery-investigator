
package com.alexbalutin.smartbattery.telemetry;

public record BatteryReading(
        Double batteryPercent,
        Integer remainingCapacity,
        String capacityUnits,
        Double voltageVolts,
        Double dischargeRateWatts,
        Boolean charging,
        Boolean discharging,
        Boolean powerOnline,
        String sourceName) {
}
