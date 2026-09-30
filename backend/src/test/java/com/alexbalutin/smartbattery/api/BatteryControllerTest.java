
package com.alexbalutin.smartbattery.api;

import com.alexbalutin.smartbattery.repository.BatterySampleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BatteryControllerTest {

    @Test
    void returnsNotFoundBeforeFirstSample() throws Exception {
        BatterySampleRepository repository = mock(BatterySampleRepository.class);
        when(repository.findLatest()).thenReturn(Optional.empty());

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new BatteryController(repository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/api/battery/current"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsOutOfRangeHistoryHours() throws Exception {
        BatterySampleRepository repository = mock(BatterySampleRepository.class);

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new BatteryController(repository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/api/battery/history").param("hours", "0"))
                .andExpect(status().isBadRequest());
    }
}
