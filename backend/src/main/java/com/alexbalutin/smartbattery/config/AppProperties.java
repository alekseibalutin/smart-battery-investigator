
package com.alexbalutin.smartbattery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Telemetry telemetry = new Telemetry();
    private final ProcessFilter processFilter = new ProcessFilter();
    private final Analysis analysis = new Analysis();
    private final Demo demo = new Demo();

    public Telemetry getTelemetry() {
        return telemetry;
    }

    public ProcessFilter getProcessFilter() {
        return processFilter;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public Demo getDemo() {
        return demo;
    }

    public static class Telemetry {
        private String mode = "windows";
        private long samplingIntervalMs = 5000;
        private long initialDelayMs = 1000;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public long getSamplingIntervalMs() {
            return samplingIntervalMs;
        }

        public void setSamplingIntervalMs(long samplingIntervalMs) {
            this.samplingIntervalMs = samplingIntervalMs;
        }

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }
    }

    public static class ProcessFilter {
        private double minimumCpuPercent = 0.5;
        private long minimumMemoryMb = 75;
        private int maximumProcessesPerSample = 80;

        public double getMinimumCpuPercent() {
            return minimumCpuPercent;
        }

        public void setMinimumCpuPercent(double minimumCpuPercent) {
            this.minimumCpuPercent = minimumCpuPercent;
        }

        public long getMinimumMemoryMb() {
            return minimumMemoryMb;
        }

        public void setMinimumMemoryMb(long minimumMemoryMb) {
            this.minimumMemoryMb = minimumMemoryMb;
        }

        public int getMaximumProcessesPerSample() {
            return maximumProcessesPerSample;
        }

        public void setMaximumProcessesPerSample(int maximumProcessesPerSample) {
            this.maximumProcessesPerSample = maximumProcessesPerSample;
        }
    }

    public static class Analysis {
        private int rollingWindowMinutes = 5;
        private int historicalBaselineHours = 24;
        private double minimumHighDrainPercentPerHour = 10.0;
        private double standardDeviationMultiplier = 1.5;
        private int minimumBaselineWindows = 3;
        private long intervalMergeGapSeconds = 30;
        private int processBaselineMinutes = 30;
        private int maximumAssociatedProcesses = 10;

        public int getRollingWindowMinutes() {
            return rollingWindowMinutes;
        }

        public void setRollingWindowMinutes(int rollingWindowMinutes) {
            this.rollingWindowMinutes = rollingWindowMinutes;
        }

        public int getHistoricalBaselineHours() {
            return historicalBaselineHours;
        }

        public void setHistoricalBaselineHours(int historicalBaselineHours) {
            this.historicalBaselineHours = historicalBaselineHours;
        }

        public double getMinimumHighDrainPercentPerHour() {
            return minimumHighDrainPercentPerHour;
        }

        public void setMinimumHighDrainPercentPerHour(double minimumHighDrainPercentPerHour) {
            this.minimumHighDrainPercentPerHour = minimumHighDrainPercentPerHour;
        }

        public double getStandardDeviationMultiplier() {
            return standardDeviationMultiplier;
        }

        public void setStandardDeviationMultiplier(double standardDeviationMultiplier) {
            this.standardDeviationMultiplier = standardDeviationMultiplier;
        }

        public int getMinimumBaselineWindows() {
            return minimumBaselineWindows;
        }

        public void setMinimumBaselineWindows(int minimumBaselineWindows) {
            this.minimumBaselineWindows = minimumBaselineWindows;
        }

        public long getIntervalMergeGapSeconds() {
            return intervalMergeGapSeconds;
        }

        public void setIntervalMergeGapSeconds(long intervalMergeGapSeconds) {
            this.intervalMergeGapSeconds = intervalMergeGapSeconds;
        }

        public int getProcessBaselineMinutes() {
            return processBaselineMinutes;
        }

        public void setProcessBaselineMinutes(int processBaselineMinutes) {
            this.processBaselineMinutes = processBaselineMinutes;
        }

        public int getMaximumAssociatedProcesses() {
            return maximumAssociatedProcesses;
        }

        public void setMaximumAssociatedProcesses(int maximumAssociatedProcesses) {
            this.maximumAssociatedProcesses = maximumAssociatedProcesses;
        }
    }

    public static class Demo {
        private long randomSeed = 42L;

        public long getRandomSeed() {
            return randomSeed;
        }

        public void setRandomSeed(long randomSeed) {
            this.randomSeed = randomSeed;
        }
    }
}
