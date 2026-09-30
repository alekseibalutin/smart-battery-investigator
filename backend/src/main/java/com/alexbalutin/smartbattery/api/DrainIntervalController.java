
package com.alexbalutin.smartbattery.api;

import com.alexbalutin.smartbattery.api.dto.ApiDtos;
import com.alexbalutin.smartbattery.api.dto.ApiMapper;
import com.alexbalutin.smartbattery.model.DrainInterval;
import com.alexbalutin.smartbattery.repository.BatterySampleRepository;
import com.alexbalutin.smartbattery.repository.DrainIntervalRepository;
import com.alexbalutin.smartbattery.repository.ProcessDrainScoreRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/drain-intervals")
public class DrainIntervalController {

    private final DrainIntervalRepository intervalRepository;
    private final BatterySampleRepository batteryRepository;
    private final ProcessDrainScoreRepository scoreRepository;

    public DrainIntervalController(
            DrainIntervalRepository intervalRepository,
            BatterySampleRepository batteryRepository,
            ProcessDrainScoreRepository scoreRepository) {
        this.intervalRepository = intervalRepository;
        this.batteryRepository = batteryRepository;
        this.scoreRepository = scoreRepository;
    }

    @GetMapping
    public List<ApiDtos.DrainIntervalDto> list(@RequestParam(defaultValue = "24") int hours) {
        int validatedHours = RequestParameters.validateHours(hours);
        Instant since = Instant.now().minus(validatedHours, ChronoUnit.HOURS);
        return intervalRepository.findSince(since).stream()
                .map(ApiMapper::interval)
                .toList();
    }

    @GetMapping("/{id}")
    public ApiDtos.DrainIntervalDetailDto detail(@PathVariable long id) {
        DrainInterval interval = intervalRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Drain interval " + id + " was not found"));

        return new ApiDtos.DrainIntervalDetailDto(
                ApiMapper.interval(interval),
                batteryRepository.findBetween(interval.startTime(), interval.endTime()).stream()
                        .map(ApiMapper::battery)
                        .toList(),
                scoreRepository.findByIntervalId(id).stream()
                        .map(ApiMapper::score)
                        .toList());
    }
}
