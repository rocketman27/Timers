package com.bnpparibas.tr.timer.web;

import com.bnpparibas.tr.timer.domain.Timer;
import com.bnpparibas.tr.timer.dto.IdsRequest;
import com.bnpparibas.tr.timer.repo.TimerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ControllerIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired TimerRepository repo;

    @BeforeEach
    void setUp() {
        repo.deleteAll();
        // name — это @Id в вашей сущности Timer
        repo.saveAll(List.of(
            new Timer("A", true),
            new Timer("B", false),
            new Timer("C", true)
        ));
    }

    @Test
    void suspend_partial_success() throws Exception {
        // A существует, Z нет → PARTIAL
        var body = om.writeValueAsString(new IdsRequest(List.of("A", "Z")));

        mvc.perform(post("/api/timers/suspend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
           .andExpect(status().is(206))
           .andExpect(jsonPath("$.actionStatus").value("PARTIAL"))
           .andExpect(jsonPath("$.message", containsString("suspended")))
           .andExpect(jsonPath("$.processedIds", contains("A")))
           .andExpect(jsonPath("$.failedIds", contains("Z")));

        Assertions.assertFalse(repo.findById("A").orElseThrow().isActive());
    }

    @Test
    void resume_successful() throws Exception {
        // B был inactive → станет active
        var body = om.writeValueAsString(new IdsRequest(List.of("B")));

        mvc.perform(post("/api/timers/resume")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.actionStatus").value("SUCCESSFUL"))
           .andExpect(jsonPath("$.processedIds", contains("B")))
           .andExpect(jsonPath("$.failedIds", empty()));

        Assertions.assertTrue(repo.findById("B").orElseThrow().isActive());
    }

    @Test
    void trigger_error_all_failed() throws Exception {
        var body = om.writeValueAsString(new IdsRequest(List.of("XXX", "YYY")));

        mvc.perform(post("/api/timers/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.actionStatus").value("ERROR"))
           .andExpect(jsonPath("$.processedIds", empty()))
           .andExpect(jsonPath("$.failedIds", containsInAnyOrder("XXX","YYY")));
    }

    @Test
    void validation_empty_ids_returns_400() throws Exception {
        var body = om.writeValueAsString(new IdsRequest(List.of()));

        mvc.perform(post("/api/timers/suspend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
           .andExpect(status().isBadRequest())
           // если у тебя есть @ControllerAdvice, можно дополнительно проверить message:
           // .andExpect(jsonPath("$.message", containsString("must not be empty")))
           ;
    }

    @Test
    void validation_blank_id_returns_400() throws Exception {
        var body = om.writeValueAsString(new IdsRequest(List.of("  ")));

        mvc.perform(post("/api/timers/resume")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
           .andExpect(status().isBadRequest());
    }
}