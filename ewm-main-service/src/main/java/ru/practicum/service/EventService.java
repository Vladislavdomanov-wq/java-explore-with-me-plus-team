package ru.practicum.service;

import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;

import java.util.List;

public interface EventService {
    List<EventShortDto> getPublishedEvents();

    EventFullDto getPublishedEvent(Long id);
}