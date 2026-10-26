package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHit;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StatsRecorder {

    private static final String APP_NAME = "ewm-main-service";

    private final StatsClient statsClient;

    public void record(HttpServletRequest request) {
        EndpointHit hit = new EndpointHit();
        hit.setApp(APP_NAME);
        hit.setUri(request.getRequestURI());
        hit.setIp(resolveClientIp(request));
        hit.setTimestamp(LocalDateTime.now());
        statsClient.saveHit(hit);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}