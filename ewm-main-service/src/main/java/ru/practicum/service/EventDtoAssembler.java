package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.UserShortDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.Event;
import ru.practicum.model.RequestStatus;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.RequestRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventDtoAssembler {

    private static final LocalDateTime STATS_START = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    public EventFullDto toFull(Event event) {
        Map<Long, Long> views = getViews(List.of(event));
        return EventMapper.toFullDto(
                event,
                getCategory(event.getCategoryId()),
                getInitiator(event.getInitiatorId()),
                getConfirmed(event.getId()),
                views.getOrDefault(event.getId(), 0L)
        );
    }

    public EventShortDto toShort(Event event) {
        Map<Long, Long> views = getViews(List.of(event));
        return EventMapper.toShortDto(
                event,
                getCategory(event.getCategoryId()),
                getInitiator(event.getInitiatorId()),
                getConfirmed(event.getId()),
                views.getOrDefault(event.getId(), 0L)
        );
    }

    public List<EventFullDto> toFullList(List<Event> events) {
        Map<Long, Long> views = getViews(events);
        Map<Long, CategoryDto> categories = new HashMap<>();
        Map<Long, UserShortDto> initiators = new HashMap<>();
        return events.stream()
                .map(event -> EventMapper.toFullDto(
                        event,
                        categories.computeIfAbsent(event.getCategoryId(), this::getCategory),
                        initiators.computeIfAbsent(event.getInitiatorId(), this::getInitiator),
                        getConfirmed(event.getId()),
                        views.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    public List<EventShortDto> toShortList(List<Event> events) {
        Map<Long, Long> views = getViews(events);
        Map<Long, CategoryDto> categories = new HashMap<>();
        Map<Long, UserShortDto> initiators = new HashMap<>();
        return events.stream()
                .map(event -> EventMapper.toShortDto(
                        event,
                        categories.computeIfAbsent(event.getCategoryId(), this::getCategory),
                        initiators.computeIfAbsent(event.getInitiatorId(), this::getInitiator),
                        getConfirmed(event.getId()),
                        views.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    private long getConfirmed(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    private Map<Long, Long> getViews(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .distinct()
                .toList();
        List<ViewStats> stats = statsClient.getStats(STATS_START, LocalDateTime.now(), uris, true);
        Map<Long, Long> views = new HashMap<>();
        for (Event event : events) {
            String uri = "/events/" + event.getId();
            long hits = stats.stream()
                    .filter(stat -> uri.equals(stat.getUri()))
                    .mapToLong(stat -> stat.getHits() == null ? 0L : stat.getHits())
                    .sum();
            views.put(event.getId(), hits);
        }
        return views;
    }

    private CategoryDto getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(CategoryMapper::toDto)
                .orElse(null);
    }

    private UserShortDto getInitiator(Long userId) {
        return userRepository.findById(userId)
                .map(UserMapper::toShortDto)
                .orElse(null);
    }
}
