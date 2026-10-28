package ru.practicum.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StatsControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void saveHit_shouldReturnCreated() throws Exception {
        String hitJson = new String(Files.readAllBytes(
                new ClassPathResource("hit-request.json").getFile().toPath()
        ));

        mockMvc.perform(post("/hit")
                        .contentType("application/json")
                        .content(hitJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.app").value("ewm-main-service"));
    }

    @Test
    void getStats_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2026-10-01 00:00:00")
                        .param("end", "2026-10-31 23:59:59")
                        .param("uris", "/events/1")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}