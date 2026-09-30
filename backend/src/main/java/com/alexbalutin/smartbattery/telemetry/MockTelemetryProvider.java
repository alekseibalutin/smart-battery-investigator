
package com.alexbalutin.smartbattery.telemetry;

import com.alexbalutin.smartbattery.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@ConditionalOnProperty(name = "app.telemetry.mode", havingValue = "mock")
public class MockTelemetryProvider implements TelemetryProvider {

    private static final Logger log = LoggerFactory.getLogger(MockTelemetryProvider.class);
    private static final long MIB = 1024L * 1024L;

    private final AppProperties properties;
    private final Random random;

    private long sampleIndex;
    private double batteryPercent = 87.0;

    public MockTelemetryProvider(AppProperties properties) {
        this.properties = properties;
        this.random = new Random(properties.getDemo().getRandomSeed());
        log.info("Mock telemetry mode enabled; generated values are synthetic demo data");
    }

    @Override
    public synchronized TelemetrySnapshot collect() {
        boolean charging = isChargingPhase();
        boolean highDrain = isHighDrainPhase();

        double intervalHours = properties.getTelemetry().getSamplingIntervalMs() / 3_600_000.0;
        double ratePercentPerHour = charging ? -22.0 : highDrain ? 19.0 : 6.0;
        double change = ratePercentPerHour * intervalHours;

        batteryPercent = clamp(batteryPercent - change + jitter(0.004), 8.0, 100.0);
        if (batteryPercent <= 9.0 && !charging) {
            sampleIndex = 300;
        }

        double watts = charging ? 0.0 : highDrain ? 27.0 + jitter(1.2) : 9.5 + jitter(0.8);
        BatteryReading battery = new BatteryReading(
                round(batteryPercent, 2),
                (int) Math.round(batteryPercent * 500.0),
                "MWH",
                11.7 + jitter(0.08),
                charging ? null : Math.max(1.0, watts),
                charging,
                !charging,
                charging,
                "Mock Li-ion Battery");

        List<ProcessReading> processes = buildProcesses(highDrain, charging);
        sampleIndex++;
        return new TelemetrySnapshot(Instant.now(), battery, processes);
    }

    @Override
    public String mode() {
        return "mock";
    }

    private List<ProcessReading> buildProcesses(boolean highDrain, boolean charging) {
        List<ProcessReading> readings = new ArrayList<>();
        readings.add(process(4120, "chrome.exe", highDrain ? 34.0 : 7.0, 1250, highDrain));
        readings.add(process(5288, "Code.exe", highDrain ? 18.0 : 5.5, 820, highDrain));
        readings.add(process(7712, "Discord.exe", highDrain ? 10.5 : 2.0, 510, highDrain));
        readings.add(process(8944, "java.exe", highDrain ? 15.0 : 3.0, 690, highDrain));
        readings.add(process(6320, "Teams.exe", highDrain ? 8.5 : 1.8, 640, highDrain));
        readings.add(process(1320, "explorer.exe", charging ? 1.5 : 0.8, 220, false));
        return readings;
    }

    private ProcessReading process(int pid, String name, double cpuBase, long memoryMiB, boolean busy) {
        double cpu = Math.max(0.1, cpuBase + jitter(busy ? 2.5 : 1.0));
        long memory = Math.max(32L * MIB,
                (memoryMiB + Math.round(jitter(20.0))) * MIB);
        long startTime = Instant.now().minusSeconds(3600 + pid % 1800).toEpochMilli();
        long cumulativeCpu = Math.max(1000L, (sampleIndex + 1) * Math.round(cpu * 50.0));

        return new ProcessReading(
                pid,
                name,
                round(cpu, 2),
                memory,
                "C:\\Demo\\" + name,
                startTime,
                cumulativeCpu);
    }

    private boolean isChargingPhase() {
        long phase = sampleIndex % 360;
        return phase >= 300 && phase < 336;
    }

    private boolean isHighDrainPhase() {
        long phase = sampleIndex % 360;
        return (phase >= 35 && phase < 105) || (phase >= 190 && phase < 245);
    }

    private double jitter(double magnitude) {
        return random.nextGaussian() * magnitude;
    }

    private static double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
