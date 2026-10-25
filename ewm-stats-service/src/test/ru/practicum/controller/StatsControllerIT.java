package ru.practicum.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
        String hitJson = "{" +
                "\"app\":\"ewm-main-service\"," +
                "\"uri\":\"/events/1\"," +
                "\"ip\":\"192.163.0.1\"," +
                "\"timestamp\":\"2026-10-24 15:30:00\"" +
                "}";

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hitJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.app").value("ewm-main-service"));
    }

    @Test
    void getStats_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2026-10-24 00:00:00")
                        .param("end", "2026-10-24 23:59:59")
                        .param("unique", "false"))
                .andExpect(status().isOk());
    }
}