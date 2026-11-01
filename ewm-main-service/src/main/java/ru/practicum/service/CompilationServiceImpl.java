package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.dto.UpdateCompilationRequest;
import ru.practicum.dto.UserShortDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.model.RequestStatus;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;


    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto request) {
        Compilation compilation = Compilation.builder()
                .pinned(Boolean.TRUE.equals(request.getPinned()))
                .title(request.getTitle())
                .events(new LinkedHashSet<>(resolveEvents(request.getEvents())))
                .build();
        return toDto(compilationRepository.save(compilation));
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest request) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compilationId + " was not found"));

        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        if (request.getEvents() != null) {
            compilation.setEvents(new LinkedHashSet<>(resolveEvents(request.getEvents())));
        }
        return toDto(compilationRepository.save(compilation));
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compilationId + " was not found"));
        compilationRepository.delete(compilation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        int offset = from == null ? 0 : from;
        int limit = size == null ? 10 : size;
        List<Compilation> compilations = compilationRepository
                .findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(compilation -> pinned == null || pinned.equals(compilation.getPinned()))
                .toList();
        int start = Math.min(offset, compilations.size());
        int end = Math.min(start + limit, compilations.size());
        return compilations.subList(start, end).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilation(Long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compilationId + " was not found"));
        return toDto(compilation);
    }

    private long getConfirmedRequests(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    private Map<Long, Long> getViewsMap(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        List<ViewStats> stats = statsClient.getStats(
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now(),
                uris,
                true
        );

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

    private List<Event> resolveEvents(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(eventIds);
        List<Event> events = new ArrayList<>(eventRepository.findAllById(uniqueIds));
        if (events.size() != uniqueIds.size()) {
            throw new NotFoundException("One or more events were not found");
        }
        return events;
    }

    private CompilationDto toDto(Compilation compilation) {
        List<Event> events = new ArrayList<>(compilation.getEvents());
        events.sort((a, b) -> Long.compare(a.getId(), b.getId()));

        Map<Long, Long> views = getViewsMap(events);

        List<EventShortDto> eventDtos = events.stream()
                .map(e -> EventMapper.toShortDto(
                        e,
                        new CategoryDto(e.getCategory().getId(), e.getCategory().getName()),
                        new UserShortDto(e.getInitiator().getId(), e.getInitiator().getName()),
                        getConfirmedRequests(e.getId()),
                        views.getOrDefault(e.getId(), 0L)
                ))
                .collect(Collectors.toList());

        return CompilationDto.builder()
                .id(compilation.getId())
                .events(eventDtos)
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }
}
