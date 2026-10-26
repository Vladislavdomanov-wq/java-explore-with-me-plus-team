package ru.practicum.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.service.EventService;
import ru.practicum.service.StatsRecorder;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final StatsRecorder statsRecorder;

    @GetMapping
    public List<EventShortDto> getEvents(HttpServletRequest request) {
        statsRecorder.record(request);
        return eventService.getPublishedEvents();
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(@PathVariable Long id, HttpServletRequest request) {
        statsRecorder.record(request);
        return eventService.getPublishedEvent(id);
    }
}