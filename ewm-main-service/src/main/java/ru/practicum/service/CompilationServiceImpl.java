package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.dto.UpdateCompilationRequest;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventDtoAssembler eventDtoAssembler;

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
        return CompilationDto.builder()
                .id(compilation.getId())
                .events(eventDtoAssembler.toShortList(events))
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }
}
