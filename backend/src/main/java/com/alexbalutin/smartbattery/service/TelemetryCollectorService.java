
package com.alexbalutin.smartbattery.service;

import com.alexbalutin.smartbattery.model.BatterySample;
import com.alexbalutin.smartbattery.repository.BatterySampleRepository;
import com.alexbalutin.smartbattery.repository.ProcessSampleRepository;
import com.alexbalutin.smartbattery.telemetry.TelemetryProvider;
import com.alexbalutin.smartbattery.telemetry.TelemetrySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TelemetryCollectorService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryCollectorService.class);

    private final TelemetryProvider telemetryProvider;
    private final BatterySampleRepository batteryRepository;
    private final ProcessSampleRepository processRepository;
    private final DrainAnalysisService drainAnalysisService;
    private final TransactionTemplate transactionTemplate;

    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicReference<Instant> lastSuccessfulCollection = new AtomicReference<>();
    private final AtomicReference<String> lastError = new AtomicReference<>();

    public TelemetryCollectorService(
            TelemetryProvider telemetryProvider,
            BatterySampleRepository batteryRepository,
            ProcessSampleRepository processRepository,
            DrainAnalysisService drainAnalysisService,
            PlatformTransactionManager transactionManager) {
        this.telemetryProvider = telemetryProvider;
        this.batteryRepository = batteryRepository;
        this.processRepository = processRepository;
        this.drainAnalysisService = drainAnalysisService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(
            fixedDelayString = "${app.telemetry.sampling-interval-ms:5000}",
            initialDelayString = "${app.telemetry.initial-delay-ms:1000}")
    public void collectScheduledSample() {
        active.set(true);
        try {
            TelemetrySnapshot snapshot = telemetryProvider.collect();

            BatterySample saved = transactionTemplate.execute(status -> {
                BatterySample battery = batteryRepository.insert(
                        snapshot.timestamp(),
                        snapshot.battery(),
                        telemetryProvider.mode());
                processRepository.insertBatch(
                        battery.id(),
                        snapshot.timestamp(),
                        snapshot.processes());
                return battery;
            });

            if (saved != null) {
                drainAnalysisService.analyzeAt(saved.timestamp());
                lastSuccessfulCollection.set(saved.timestamp());
                lastError.set(null);
            }
        } catch (Exception ex) {
            lastError.set(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            log.error("Telemetry collection failed; the collector will retry on the next interval", ex);
        }
    }

    public boolean isActive() {
        return active.get();
    }

    public Instant getLastSuccessfulCollection() {
        return lastSuccessfulCollection.get();
    }

    public String getLastError() {
        return lastError.get();
    }

    public String getTelemetryMode() {
        return telemetryProvider.mode();
    }
}
