package ru.practicum.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StatsClientTest {

    private MockRestServiceServer mockServer;
    private StatsClient statsClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        statsClient = new StatsClient(restTemplate, "http://localhost:9090");
    }

    @Test
    void saveHit_sendsPostWithBody() {
        EndpointHit hit = new EndpointHit();
        hit.setApp("ewm-main-service");
        hit.setUri("/events/1");
        hit.setIp("127.0.0.1");
        hit.setTimestamp(LocalDateTime.of(2026, 10, 26, 22, 0));

        mockServer.expect(requestTo("http://localhost:9090/hit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.app").value("ewm-main-service"))
                .andExpect(jsonPath("$.uri").value("/events/1"))
                .andExpect(jsonPath("$.ip").value("127.0.0.1"))
                .andRespond(withSuccess());

        statsClient.saveHit(hit);
        mockServer.verify();
    }

    @Test
    void getStats_buildsQueryWithRepeatedUris() {
        mockServer.expect(requestTo(containsString("uris=/events/1")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"app\":\"ewm-main-service\",\"uri\":\"/events/1\",\"hits\":5}]",
                        MediaType.APPLICATION_JSON));

        List<ViewStats> result = statsClient.getStats(
                "2026-10-01 00:00:00",
                "2026-10-31 23:59:59",
                List.of("/events/1"),
                false
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHits()).isEqualTo(5L);
        mockServer.verify();
    }

    @Test
    void getStats_withMultipleUris_usesRepeatedParams() {
        mockServer.expect(requestTo(containsString("uris=/events/1")))
                .andExpect(requestTo(containsString("uris=/events/2")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        statsClient.getStats("2026-10-01 00:00:00", "2026-10-31 23:59:59",
                List.of("/events/1", "/events/2"), true);

        mockServer.verify();
    }
}