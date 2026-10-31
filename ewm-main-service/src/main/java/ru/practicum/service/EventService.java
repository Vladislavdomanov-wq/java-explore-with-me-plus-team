package ru.practicum.service;

import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewEventDto;
import ru.practicum.dto.UpdateEventAdminRequest;
import ru.practicum.dto.UpdateEventUserRequest;

import java.util.List;

public interface EventService {

    EventFullDto createEvent(Long userId, NewEventDto dto);

    List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest dto);

    List<EventFullDto> adminSearchEvents(List<Long> users, List<String> states,
                                         List<Long> categories,
                                         String rangeStart, String rangeEnd,
                                         Integer from, Integer size);

    EventFullDto adminUpdateEvent(Long eventId, UpdateEventAdminRequest dto);

    List<EventShortDto> searchEvents(String text, List<Long> categories, Boolean paid,
                                     String rangeStart, String rangeEnd,
                                     Boolean onlyAvailable, String sort,
                                     Integer from, Integer size);

    EventFullDto getPublishedEvent(Long id);
}