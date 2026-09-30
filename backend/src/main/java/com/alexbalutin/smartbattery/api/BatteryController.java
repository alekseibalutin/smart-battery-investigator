
package com.alexbalutin.smartbattery.api;

import com.alexbalutin.smartbattery.api.dto.ApiDtos;
import com.alexbalutin.smartbattery.api.dto.ApiMapper;
import com.alexbalutin.smartbattery.repository.BatterySampleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/battery")
public class BatteryController {

    private final BatterySampleRepository batteryRepository;

    public BatteryController(BatterySampleRepository batteryRepository) {
        this.batteryRepository = batteryRepository;
    }

    @GetMapping("/current")
    public ApiDtos.BatterySampleDto current() {
        return batteryRepository.findLatest()
                .map(ApiMapper::battery)
                .orElseThrow(() -> new NoSuchElementException("No battery telemetry has been collected yet"));
    }

    @GetMapping("/history")
    public List<ApiDtos.BatterySampleDto> history(@RequestParam(defaultValue = "24") int hours) {
        int validatedHours = RequestParameters.validateHours(hours);
        Instant since = Instant.now().minus(validatedHours, ChronoUnit.HOURS);
        return batteryRepository.findSince(since).stream()
                .map(ApiMapper::battery)
                .toList();
    }
}
