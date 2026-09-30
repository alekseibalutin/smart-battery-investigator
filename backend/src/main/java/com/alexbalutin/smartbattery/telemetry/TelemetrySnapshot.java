
package com.alexbalutin.smartbattery.telemetry;

import java.time.Instant;
import java.util.List;

public record TelemetrySnapshot(
        Instant timestamp,
        BatteryReading battery,
        List<ProcessReading> processes) {
}
