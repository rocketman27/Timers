// src/test/java/.../web/TimerControllerTest.java
package com.example.timers.web;

import com.example.timers.dto.IdsRequest;
import com.example.timers.dto.OperationResultDTO;
import com.example.timers.service.TimerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimerController.class)
class TimerControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean TimerService timerService;

    @Test
    @DisplayName("suspend: 200 OK и корректный JSON при полном успехе")
    void suspend_ok() throws Exception {
        var req = new IdsRequest(List.of("A","B"));
        var res = new OperationResultDTO("OK", "2 timers suspended",
                List.of("A","B"), List.of());

        Mockito.when(timerService.suspend(eq(req.ids()))).thenReturn(res);

        mvc.perform(post("/api/timers/suspend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("OK"))
           .andExpect(jsonPath("$.message").value("2 timers suspended"))
           .andExpect(jsonPath("$.processedIds", hasSize(2)))
           .andExpect(jsonPath("$.failedIds", empty()));
    }

    @Test
    @DisplayName("resume: 206 Partial Content при частичном успехе")
    void resume_partial() throws Exception {
        var req = new IdsRequest(List.of("A","B","C"));
        var res = new OperationResultDTO("PARTIAL", "2 resumed, 1 failed",
                List.of("A","B"), List.of("C"));

        Mockito.when(timerService.resume(eq(req.ids()))).thenReturn(res);

        mvc.perform(post("/api/timers/resume")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().is(206))
           .andExpect(jsonPath("$.status").value("PARTIAL"))
           .andExpect(jsonPath("$.processedIds", containsInAnyOrder("A","B")))
           .andExpect(jsonPath("$.failedIds", contains("C")));
    }

    @Test
    @DisplayName("trigger: 400 Bad Request при полной ошибке")
    void trigger_error() throws Exception {
        var req = new IdsRequest(List.of("X","Y"));
        var res = new OperationResultDTO("ERROR", "All operations failed",
                List.of(), List.of("X","Y"));

        Mockito.when(timerService.trigger(eq(req.ids()))).thenReturn(res);

        mvc.perform(post("/api/timers/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.status").value("ERROR"))
           .andExpect(jsonPath("$.failedIds", hasSize(2)));
    }

    @Test
    @DisplayName("Валидация: пустой список id -> 400 с сообщением")
    void validation_emptyIds() throws Exception {
        var req = new IdsRequest(List.of()); // пусто

        mvc.perform(post("/api/timers/suspend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isBadRequest()); // далее можно добавить jsonPath на сообщение, если есть handler
    }

    @Test
    @DisplayName("Валидация: id из пробелов -> 400")
    void validation_blankId() throws Exception {
        var req = new IdsRequest(List.of("  "));

        mvc.perform(post("/api/timers/suspend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isBadRequest());
    }
}