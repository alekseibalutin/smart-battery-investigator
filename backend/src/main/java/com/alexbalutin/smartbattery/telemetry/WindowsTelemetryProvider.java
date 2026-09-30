
package com.alexbalutin.smartbattery.telemetry;

import com.alexbalutin.smartbattery.config.AppProperties;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PowerSource;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.telemetry.mode", havingValue = "windows", matchIfMissing = true)
public class WindowsTelemetryProvider implements TelemetryProvider {

    private static final Logger log = LoggerFactory.getLogger(WindowsTelemetryProvider.class);
    private static final long BYTES_PER_MIB = 1024L * 1024L;

    private final AppProperties properties;
    private final HardwareAbstractionLayer hardware;
    private final OperatingSystem operatingSystem;
    private final int logicalProcessorCount;

    private Map<Integer, OSProcess> previousProcesses = new HashMap<>();
    private boolean nativeMetricWarningLogged;

    public WindowsTelemetryProvider(AppProperties properties) {
        this.properties = properties;
        SystemInfo systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.operatingSystem = systemInfo.getOperatingSystem();
        this.logicalProcessorCount = Math.max(1, hardware.getProcessor().getLogicalProcessorCount());
        log.info("Windows telemetry provider initialized for {} {}", operatingSystem.getFamily(),
                operatingSystem.getVersionInfo());
    }

    @Override
    public synchronized TelemetrySnapshot collect() {
        Instant now = Instant.now();
        BatteryReading battery = collectBattery();
        List<ProcessReading> processes = collectProcesses();
        return new TelemetrySnapshot(now, battery, processes);
    }

    @Override
    public String mode() {
        return "windows";
    }

    private BatteryReading collectBattery() {
        List<PowerSource> sources = hardware.getPowerSources();
        NativePowerStatus nativeStatus = queryNativePowerStatus();

        if (sources.isEmpty()) {
            if (nativeStatus != null && nativeStatus.batteryPercent() != null) {
                return new BatteryReading(
                        nativeStatus.batteryPercent(),
                        null,
                        null,
                        null,
                        null,
                        nativeStatus.charging(),
                        nativeStatus.discharging(),
                        nativeStatus.powerOnline(),
                        "Windows GetSystemPowerStatus");
            }

            return new BatteryReading(
                    null, null, null, null, null,
                    null, null,
                    nativeStatus == null ? null : nativeStatus.powerOnline(),
                    "Battery unavailable");
        }

        PowerSource source = sources.get(0);
        double remainingFraction = source.getRemainingCapacityPercent();
        Double batteryPercent = finiteNonNegative(remainingFraction)
                ? clamp(remainingFraction * 100.0, 0.0, 100.0)
                : null;

        if (batteryPercent == null && nativeStatus != null) {
            batteryPercent = nativeStatus.batteryPercent();
        }

        Integer remainingCapacity = source.getCurrentCapacity() >= 0
                ? source.getCurrentCapacity()
                : null;

        String capacityUnits = source.getCapacityUnits() == null
                ? null
                : source.getCapacityUnits().toString();

        Double voltage = source.getVoltage() > 0.0 && Double.isFinite(source.getVoltage())
                ? source.getVoltage()
                : null;

        boolean charging = source.isCharging();
        boolean discharging = source.isDischarging();
        boolean powerOnline = source.isPowerOnLine();

        if (!charging && !discharging && nativeStatus != null) {
            charging = Boolean.TRUE.equals(nativeStatus.charging());
            discharging = Boolean.TRUE.equals(nativeStatus.discharging());
        }
        if (!powerOnline && nativeStatus != null && nativeStatus.powerOnline() != null) {
            powerOnline = nativeStatus.powerOnline();
        }

        double rawPowerMilliwatts = source.getPowerUsageRate();
        Double dischargeRateWatts = null;
        if (discharging && Double.isFinite(rawPowerMilliwatts) && rawPowerMilliwatts != 0.0) {
            dischargeRateWatts = Math.abs(rawPowerMilliwatts) / 1000.0;
        }

        return new BatteryReading(
                batteryPercent,
                remainingCapacity,
                capacityUnits,
                voltage,
                dischargeRateWatts,
                charging,
                discharging,
                powerOnline,
                source.getName());
    }

    private List<ProcessReading> collectProcesses() {
        List<OSProcess> currentProcesses = operatingSystem.getProcesses();
        Map<Integer, OSProcess> nextPrevious = new HashMap<>(currentProcesses.size());
        List<ProcessReading> readings = new ArrayList<>();

        double minimumCpu = properties.getProcessFilter().getMinimumCpuPercent();
        long minimumMemoryBytes = properties.getProcessFilter().getMinimumMemoryMb() * BYTES_PER_MIB;

        for (OSProcess process : currentProcesses) {
            if (process == null || process.getState() == OSProcess.State.INVALID) {
                continue;
            }

            int pid = process.getProcessID();
            nextPrevious.put(pid, process);

            OSProcess previous = previousProcesses.get(pid);
            double cpuFraction = previous == null
                    ? process.getProcessCpuLoadCumulative()
                    : process.getProcessCpuLoadBetweenTicks(previous);

            double cpuPercent = Math.max(0.0,
                    Math.min(100.0, cpuFraction * 100.0 / logicalProcessorCount));

            long memoryBytes = Math.max(0L, process.getResidentMemory());
            if (cpuPercent < minimumCpu && memoryBytes < minimumMemoryBytes) {
                continue;
            }

            String name = safeName(process.getName(), pid);
            String path = blankToNull(process.getPath());
            long startTime = process.getStartTime();
            Long nullableStartTime = startTime > 0 ? startTime : null;
            long cumulativeCpuMs = Math.max(0L, process.getKernelTime() + process.getUserTime());

            readings.add(new ProcessReading(
                    pid,
                    name,
                    cpuPercent,
                    memoryBytes,
                    path,
                    nullableStartTime,
                    cumulativeCpuMs));
        }

        previousProcesses = nextPrevious;

        Comparator<ProcessReading> comparator = Comparator
                .comparingDouble(ProcessReading::cpuUsagePercent)
                .thenComparingLong(ProcessReading::memoryBytes)
                .reversed();

        return readings.stream()
                .sorted(comparator)
                .limit(properties.getProcessFilter().getMaximumProcessesPerSample())
                .toList();
    }

    private NativePowerStatus queryNativePowerStatus() {
        if (!Platform.isWindows()) {
            if (!nativeMetricWarningLogged) {
                log.warn("Windows-native power metrics are unavailable because the application is not running on Windows");
                nativeMetricWarningLogged = true;
            }
            return null;
        }

        try {
            SystemPowerStatus status = new SystemPowerStatus();
            if (!Kernel32PowerApi.INSTANCE.GetSystemPowerStatus(status)) {
                return null;
            }
            status.read();

            int percentRaw = Byte.toUnsignedInt(status.BatteryLifePercent);
            Double percent = percentRaw == 255 ? null : (double) percentRaw;

            int acRaw = Byte.toUnsignedInt(status.ACLineStatus);
            Boolean online = acRaw == 255 ? null : acRaw == 1;

            int flag = Byte.toUnsignedInt(status.BatteryFlag);
            Boolean charging = flag == 255 ? null : (flag & 8) != 0;
            Boolean discharging = online == null ? null : !online && percent != null;

            return new NativePowerStatus(percent, charging, discharging, online);
        } catch (UnsatisfiedLinkError | RuntimeException ex) {
            if (!nativeMetricWarningLogged) {
                log.warn("Windows GetSystemPowerStatus is unavailable; continuing with OSHI-only battery metrics: {}",
                        ex.getMessage());
                nativeMetricWarningLogged = true;
            }
            return null;
        }
    }

    private static boolean finiteNonNegative(double value) {
        return Double.isFinite(value) && value >= 0.0;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static String safeName(String name, int pid) {
        return name == null || name.isBlank() ? "pid-" + pid : name;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record NativePowerStatus(
            Double batteryPercent,
            Boolean charging,
            Boolean discharging,
            Boolean powerOnline) {
    }

    private interface Kernel32PowerApi extends StdCallLibrary {
        Kernel32PowerApi INSTANCE = Native.load("Kernel32", Kernel32PowerApi.class);

        boolean GetSystemPowerStatus(SystemPowerStatus status);
    }

    public static class SystemPowerStatus extends Structure {
        public byte ACLineStatus;
        public byte BatteryFlag;
        public byte BatteryLifePercent;
        public byte Reserved1;
        public int BatteryLifeTime;
        public int BatteryFullLifeTime;

        @Override
        protected List<String> getFieldOrder() {
            return List.of(
                    "ACLineStatus",
                    "BatteryFlag",
                    "BatteryLifePercent",
                    "Reserved1",
                    "BatteryLifeTime",
                    "BatteryFullLifeTime");
        }
    }
}
