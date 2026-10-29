package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class StatsClient {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public StatsClient(RestTemplate restTemplate,
                       @Value("${stats-server.url:http://localhost:9090}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void saveHit(EndpointHit hit) {
        HttpEntity<EndpointHit> request = new HttpEntity<>(hit);
        try {
            restTemplate.postForEntity(baseUrl + "/hit", request, Void.class);
        } catch (Exception e) {
            log.warn("Failed to send hit to stats service: {}", e.getMessage());
        }
    }

    /** Сигнатура из ТЗ. start/end — строки в формате "yyyy-MM-dd HH:mm:ss". */
    public List<ViewStats> getStats(String start, String end,
                                    List<String> uris, Boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/stats")
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("unique", Boolean.TRUE.equals(unique));

        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", uris.toArray());   // ?uris=/a&uris=/b
        }

        try {
            ResponseEntity<List<ViewStats>> response = restTemplate.exchange(
                    builder.build().encode().toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (Exception e) {
            log.warn("Failed to get stats: {}", e.getMessage());
            return List.of();
        }
    }

    /** Удобная обёртка для вызовов из кода. */
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    List<String> uris, boolean unique) {
        return getStats(start.format(FORMATTER), end.format(FORMATTER), uris, unique);
    }
}