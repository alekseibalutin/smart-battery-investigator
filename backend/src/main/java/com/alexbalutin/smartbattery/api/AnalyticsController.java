
package com.alexbalutin.smartbattery.api;

import com.alexbalutin.smartbattery.analysis.TelemetryAggregationService;
import com.alexbalutin.smartbattery.api.dto.ApiDtos;
import com.alexbalutin.smartbattery.api.dto.ApiMapper;
import com.alexbalutin.smartbattery.config.AppProperties;
import com.alexbalutin.smartbattery.model.DrainInterval;
import com.alexbalutin.smartbattery.repository.BatterySampleRepository;
import com.alexbalutin.smartbattery.repository.DrainIntervalRepository;
import com.alexbalutin.smartbattery.repository.ProcessDrainScoreRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final BatterySampleRepository batteryRepository;
    private final DrainIntervalRepository intervalRepository;
    private final ProcessDrainScoreRepository scoreRepository;
    private final TelemetryAggregationService aggregationService;
    private final AppProperties properties;

    public AnalyticsController(
            BatterySampleRepository batteryRepository,
            DrainIntervalRepository intervalRepository,
            ProcessDrainScoreRepository scoreRepository,
            TelemetryAggregationService aggregationService,
            AppProperties properties) {
        this.batteryRepository = batteryRepository;
        this.intervalRepository = intervalRepository;
        this.scoreRepository = scoreRepository;
        this.aggregationService = aggregationService;
        this.properties = properties;
    }

    @GetMapping("/summary")
    public ApiDtos.AnalyticsSummaryDto summary(@RequestParam(defaultValue = "24") int hours) {
        int validatedHours = RequestParameters.validateHours(hours);
        Instant since = Instant.now().minus(validatedHours, ChronoUnit.HOURS);

        List<DrainInterval> intervals = intervalRepository.findSince(since);
        TelemetryAggregationService.SummaryStats stats = aggregationService.summarize(
                batteryRepository.findSince(since),
                intervals,
                Duration.ofMinutes(properties.getAnalysis().getRollingWindowMinutes()));

        return new ApiDtos.AnalyticsSummaryDto(
                validatedHours,
                stats.averageDrainRatePercentPerHour(),
                stats.maximumDrainRatePercentPerHour(),
                stats.highDrainEventCount(),
                scoreRepository.findRanking(since, 5).stream()
                        .map(ApiMapper::ranking)
                        .toList());
    }
}
