
package com.alexbalutin.smartbattery.analysis;

import com.alexbalutin.smartbattery.model.ProcessDrainScore;
import com.alexbalutin.smartbattery.model.ProcessSample;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessScoringServiceTest {

    private final ProcessScoringService service = new ProcessScoringService();

    @Test
    void ranksCpuSpikeAheadOfLowActivityProcess() {
        Instant t0 = Instant.parse("2026-09-29T12:00:00Z");

        List<ProcessSample> interval = List.of(
                sample(t0, "chrome.exe", 38.0, 1_200_000_000L),
                sample(t0.plusSeconds(5), "chrome.exe", 42.0, 1_250_000_000L),
                sample(t0, "Discord.exe", 4.0, 500_000_000L),
                sample(t0.plusSeconds(5), "Discord.exe", 5.0, 500_000_000L));

        List<ProcessSample> baseline = List.of(
                sample(t0.minusSeconds(60), "chrome.exe", 6.0, 1_150_000_000L),
                sample(t0.minusSeconds(60), "Discord.exe", 3.0, 480_000_000L));

        List<ProcessDrainScore> scores = service.score(7, interval, baseline, 10);

        assertEquals("chrome.exe", scores.get(0).processName());
        assertTrue(scores.get(0).score() > scores.get(1).score());
        assertTrue(scores.get(0).cpuIncreaseAboveBaseline() > 30.0);
    }

    private ProcessSample sample(Instant time, String name, double cpu, long memory) {
        return new ProcessSample(
                0,
                1,
                time,
                name.hashCode(),
                name,
                cpu,
                memory,
                null,
                time.minusSeconds(1000).toEpochMilli(),
                1000);
    }
}
