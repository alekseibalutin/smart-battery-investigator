
package com.alexbalutin.smartbattery.service;

import com.alexbalutin.smartbattery.analysis.BatteryDrainCalculator;
import com.alexbalutin.smartbattery.analysis.HighDrainDetector;
import com.alexbalutin.smartbattery.analysis.ProcessScoringService;
import com.alexbalutin.smartbattery.config.AppProperties;
import com.alexbalutin.smartbattery.model.BatterySample;
import com.alexbalutin.smartbattery.model.DrainInterval;
import com.alexbalutin.smartbattery.model.ProcessDrainScore;
import com.alexbalutin.smartbattery.model.ProcessSample;
import com.alexbalutin.smartbattery.repository.BatterySampleRepository;
import com.alexbalutin.smartbattery.repository.DrainIntervalRepository;
import com.alexbalutin.smartbattery.repository.ProcessDrainScoreRepository;
import com.alexbalutin.smartbattery.repository.ProcessSampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

@Service
public class DrainAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DrainAnalysisService.class);

    private final AppProperties properties;
    private final BatterySampleRepository batteryRepository;
    private final ProcessSampleRepository processRepository;
    private final DrainIntervalRepository intervalRepository;
    private final ProcessDrainScoreRepository scoreRepository;
    private final BatteryDrainCalculator drainCalculator;
    private final HighDrainDetector highDrainDetector;
    private final ProcessScoringService processScoringService;

    public DrainAnalysisService(
            AppProperties properties,
            BatterySampleRepository batteryRepository,
            ProcessSampleRepository processRepository,
            DrainIntervalRepository intervalRepository,
            ProcessDrainScoreRepository scoreRepository,
            BatteryDrainCalculator drainCalculator,
            HighDrainDetector highDrainDetector,
            ProcessScoringService processScoringService) {
        this.properties = properties;
        this.batteryRepository = batteryRepository;
        this.processRepository = processRepository;
        this.intervalRepository = intervalRepository;
        this.scoreRepository = scoreRepository;
        this.drainCalculator = drainCalculator;
        this.highDrainDetector = highDrainDetector;
        this.processScoringService = processScoringService;
    }

    public void analyzeAt(Instant timestamp) {
        Duration window = Duration.ofMinutes(properties.getAnalysis().getRollingWindowMinutes());
        Instant requestedWindowStart = timestamp.minus(window);
        List<BatterySample> currentSamples = batteryRepository.findBetween(requestedWindowStart, timestamp);

        if (!coversEnoughOfWindow(currentSamples, window)) {
            return;
        }

        OptionalDouble currentRateResult = drainCalculator.calculatePercentPerHour(currentSamples);
        if (currentRateResult.isEmpty()) {
            return;
        }

        double currentRate = currentRateResult.getAsDouble();
        Instant actualWindowStart = currentSamples.get(0).timestamp();

        Instant baselineStart = timestamp.minus(
                Duration.ofHours(properties.getAnalysis().getHistoricalBaselineHours()));
        Instant baselineEnd = actualWindowStart.minusMillis(1);

        List<Double> baselineRates = baselineEnd.isAfter(baselineStart)
                ? drainCalculator.calculateNonOverlappingWindowRates(
                        batteryRepository.findBetween(baselineStart, baselineEnd),
                        window)
                : List.of();

        HighDrainDetector.Decision decision = highDrainDetector.evaluate(
                currentRate,
                baselineRates,
                properties.getAnalysis().getMinimumHighDrainPercentPerHour(),
                properties.getAnalysis().getStandardDeviationMultiplier(),
                properties.getAnalysis().getMinimumBaselineWindows());

        if (!decision.highDrain()) {
            return;
        }

        BatterySample first = currentSamples.get(0);
        BatterySample last = currentSamples.get(currentSamples.size() - 1);
        if (first.batteryPercent() == null || last.batteryPercent() == null) {
            return;
        }

        DrainInterval interval = mergeOrCreateInterval(
                first,
                last,
                currentRate,
                decision);

        updateAssociatedProcessScores(interval);
    }

    private DrainInterval mergeOrCreateInterval(
            BatterySample first,
            BatterySample last,
            double currentRate,
            HighDrainDetector.Decision decision) {

        Optional<DrainInterval> recent = intervalRepository.findMostRecent();
        long mergeGap = properties.getAnalysis().getIntervalMergeGapSeconds();

        if (recent.isPresent()
                && !recent.get().endTime().plusSeconds(mergeGap).isBefore(first.timestamp())) {

            DrainInterval existing = recent.get();
            Instant start = existing.startTime().isBefore(first.timestamp())
                    ? existing.startTime()
                    : first.timestamp();
            double startPercent = existing.startTime().isBefore(first.timestamp())
                    ? existing.startingBatteryPercent()
                    : first.batteryPercent();

            double durationHours = Math.max(
                    1.0 / 3600.0,
                    Duration.between(start, last.timestamp()).toMillis() / 3_600_000.0);
            double combinedRate = Math.max(
                    0.0,
                    (startPercent - last.batteryPercent()) / durationHours);

            DrainInterval updated = new DrainInterval(
                    existing.id(),
                    start,
                    last.timestamp(),
                    startPercent,
                    last.batteryPercent(),
                    combinedRate,
                    moreSevere(existing.severity(), decision.severity()),
                    decision.baselineMean(),
                    decision.baselineStdDev());
            return intervalRepository.update(updated);
        }

        DrainInterval created = intervalRepository.insert(
                first.timestamp(),
                last.timestamp(),
                first.batteryPercent(),
                last.batteryPercent(),
                currentRate,
                decision.severity(),
                decision.baselineMean(),
                decision.baselineStdDev());

        log.info(
                "Detected high-drain interval {} at {}%/h from {} to {}",
                created.id(),
                Math.round(created.calculatedDrainRate() * 100.0) / 100.0,
                created.startTime(),
                created.endTime());
        return created;
    }

    private void updateAssociatedProcessScores(DrainInterval interval) {
        List<ProcessSample> intervalSamples = processRepository.findBetween(
                interval.startTime(),
                interval.endTime());

        Instant baselineStart = interval.startTime().minus(
                Duration.ofMinutes(properties.getAnalysis().getProcessBaselineMinutes()));
        List<ProcessSample> baselineSamples = processRepository.findBetween(
                baselineStart,
                interval.startTime().minusMillis(1));

        List<ProcessDrainScore> scores = processScoringService.score(
                interval.id(),
                intervalSamples,
                baselineSamples,
                properties.getAnalysis().getMaximumAssociatedProcesses());

        scoreRepository.replaceForInterval(interval.id(), scores);
    }

    private static boolean coversEnoughOfWindow(List<BatterySample> samples, Duration window) {
        if (samples.size() < 2) {
            return false;
        }
        Duration covered = Duration.between(
                samples.get(0).timestamp(),
                samples.get(samples.size() - 1).timestamp());
        Duration minimum = window.multipliedBy(3).dividedBy(4);
        return !covered.minus(minimum).isNegative();
    }

    private static String moreSevere(String first, String second) {
        return severityRank(first) >= severityRank(second) ? first : second;
    }

    private static int severityRank(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 3;
            case "HIGH" -> 2;
            default -> 1;
        };
    }
}
