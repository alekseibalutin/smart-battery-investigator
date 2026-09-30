
package com.alexbalutin.smartbattery.model;

import java.time.Instant;

public record ProcessSample(
        long id,
        long batterySampleId,
        Instant timestamp,
        int pid,
        String processName,
        double cpuUsagePercent,
        long memoryBytes,
        String executablePath,
        Long processStartTimeEpochMs,
        long cumulativeCpuMs) {
}
