
package com.alexbalutin.smartbattery.telemetry;

public record ProcessReading(
        int pid,
        String processName,
        double cpuUsagePercent,
        long memoryBytes,
        String executablePath,
        Long processStartTimeEpochMs,
        long cumulativeCpuMs) {
}
