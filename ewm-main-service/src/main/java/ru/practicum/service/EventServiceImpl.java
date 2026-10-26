package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;
import ru.practicum.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final StatsClient statsClient;

    @Override
    public List<EventShortDto> getPublishedEvents() {
        List<Event> events = eventRepository.findAllByState(EventState.PUBLISHED);
        return events.stream()
                .map(e -> EventMapper.toShortDto(e, getViews("/events/" + e.getId())))
                .toList();
    }

    @Override
    public EventFullDto getPublishedEvent(Long id) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id)); // TODO: кастомное исключение + 404
        return EventMapper.toFullDto(event, getViews("/events/" + id));
    }

    private long getViews(String uri) {
        List<ViewStats> stats = statsClient.getStats(
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now(),
                List.of(uri),
                true
        );
        return stats.isEmpty() ? 0L : stats.get(0).getHits();
    }
}