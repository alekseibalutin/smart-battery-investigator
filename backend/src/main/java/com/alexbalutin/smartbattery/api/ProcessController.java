
package com.alexbalutin.smartbattery.api;

import com.alexbalutin.smartbattery.api.dto.ApiDtos;
import com.alexbalutin.smartbattery.api.dto.ApiMapper;
import com.alexbalutin.smartbattery.repository.ProcessDrainScoreRepository;
import com.alexbalutin.smartbattery.repository.ProcessSampleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/processes")
public class ProcessController {

    private final ProcessSampleRepository processRepository;
    private final ProcessDrainScoreRepository scoreRepository;

    public ProcessController(
            ProcessSampleRepository processRepository,
            ProcessDrainScoreRepository scoreRepository) {
        this.processRepository = processRepository;
        this.scoreRepository = scoreRepository;
    }

    @GetMapping("/current")
    public List<ApiDtos.ProcessSampleDto> current() {
        return processRepository.findLatestSnapshot().stream()
                .map(ApiMapper::process)
                .toList();
    }

    @GetMapping("/ranking")
    public List<ApiDtos.ProcessRankingDto> ranking(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "10") int limit) {
        int validatedHours = RequestParameters.validateHours(hours);
        int validatedLimit = Math.max(1, Math.min(50, limit));
        Instant since = Instant.now().minus(validatedHours, ChronoUnit.HOURS);
        return scoreRepository.findRanking(since, validatedLimit).stream()
                .map(ApiMapper::ranking)
                .toList();
    }
}
