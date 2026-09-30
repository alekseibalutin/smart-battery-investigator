
package com.alexbalutin.smartbattery.api;

import com.alexbalutin.smartbattery.analysis.BatteryDrainCalculator;
import com.alexbalutin.smartbattery.api.dto.ApiDtos;
import com.alexbalutin.smartbattery.api.dto.ApiMapper;
import com.alexbalutin.smartbattery.config.AppProperties;
import com.alexbalutin.smartbattery.model.BatterySample;
import com.alexbalutin.smartbattery.repository.BatterySampleRepository;
import com.alexbalutin.smartbattery.service.TelemetryCollectorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final TelemetryCollectorService collectorService;
    private final BatterySampleRepository batteryRepository;
    private final BatteryDrainCalculator drainCalculator;
    private final AppProperties properties;

    public StatusController(
            TelemetryCollectorService collectorService,
            BatterySampleRepository batteryRepository,
            BatteryDrainCalculator drainCalculator,
            AppProperties properties) {
        this.collectorService = collectorService;
        this.batteryRepository = batteryRepository;
        this.drainCalculator = drainCalculator;
        this.properties = properties;
    }

    @GetMapping
    public ApiDtos.StatusDto status() {
        Optional<BatterySample> latest = batteryRepository.findLatest();
        Double currentDrainRate = latest.flatMap(sample -> {
            Duration window = Duration.ofMinutes(properties.getAnalysis().getRollingWindowMinutes());
            List<BatterySample> samples = batteryRepository.findBetween(
                    sample.timestamp().minus(window),
                    sample.timestamp());
            if (samples.size() < 2) {
                return Optional.empty();
            }
            Duration covered = Duration.between(
                    samples.get(0).timestamp(),
                    samples.get(samples.size() - 1).timestamp());
            Duration minimumCoverage = window.multipliedBy(3).dividedBy(4);
            if (covered.compareTo(minimumCoverage) < 0) {
                return Optional.empty();
            }
            OptionalDouble rate = drainCalculator.calculatePercentPerHour(samples);
            return rate.isPresent() ? Optional.of(rate.getAsDouble()) : Optional.empty();
        }).orElse(null);

        String databaseStatus;
        long sampleCount;
        try {
            databaseStatus = batteryRepository.ping() ? "UP" : "DOWN";
            sampleCount = batteryRepository.count();
        } catch (RuntimeException ex) {
            databaseStatus = "DOWN";
            sampleCount = 0L;
        }

        return new ApiDtos.StatusDto(
                collectorService.isActive(),
                collectorService.getTelemetryMode(),
                latest.map(ApiMapper::battery).orElse(null),
                currentDrainRate,
                databaseStatus,
                sampleCount,
                properties.getTelemetry().getSamplingIntervalMs(),
                collectorService.getLastSuccessfulCollection(),
                collectorService.getLastError());
    }
}
