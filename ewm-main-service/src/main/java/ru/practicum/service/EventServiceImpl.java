package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewEventDto;
import ru.practicum.dto.UpdateEventAdminRequest;
import ru.practicum.dto.UpdateEventUserRequest;
import ru.practicum.dto.ViewStats;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;
import ru.practicum.model.RequestStatus;
import ru.practicum.model.StateAction;
import ru.practicum.model.User;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.EventSpecification;
import ru.practicum.repository.RequestRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + dto.getCategory() + " не найдена"));

        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        Event event = Event.builder()
                .annotation(dto.getAnnotation())
                .category(category)
                .createdOn(LocalDateTime.now())
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .initiator(user)
                .lat(dto.getLocation().getLat())
                .lon(dto.getLocation().getLon())
                .paid(dto.getPaid() != null ? dto.getPaid() : false)
                .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
                .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
                .state(EventState.PENDING)
                .title(dto.getTitle())
                .build();

        Event saved = eventRepository.save(event);
        long views = getViewsForEvent(saved.getId());
        long confirmed = getConfirmedRequests(saved.getId());
        return EventMapper.toFullDto(saved, confirmed, views);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable).getContent();
        Map<Long, Long> views = getViewsMap(events);

        return events.stream()
                .map(e -> EventMapper.toShortDto(e, getConfirmedRequests(e.getId()), views.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не принадлежит пользователю");
        }

        long views = getViewsForEvent(eventId);
        long confirmed = getConfirmedRequests(eventId);
        return EventMapper.toFullDto(event, confirmed, views);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не принадлежит пользователю");
        }

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Можно редактировать только события в статусе PENDING или CANCELED");
        }

        if (dto.getEventDate() != null) {
            if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
            }
            event.setEventDate(dto.getEventDate());
        }

        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getLocation() != null) {
            event.setLat(dto.getLocation().getLat());
            event.setLon(dto.getLocation().getLon());
        }
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + dto.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        if (dto.getStateAction() == StateAction.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        } else if (dto.getStateAction() == StateAction.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }

        Event saved = eventRepository.save(event);
        long views = getViewsForEvent(eventId);
        long confirmed = getConfirmedRequests(eventId);
        return EventMapper.toFullDto(saved, confirmed, views);
    }

    @Override
    public List<EventFullDto> adminSearchEvents(List<Long> users, List<String> states,
                                                List<Long> categories, String rangeStart,
                                                String rangeEnd, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "eventDate"));

        Specification<Event> spec = Specification
                .where(EventSpecification.hasInitiators(users))
                .and(EventSpecification.hasStates(states))
                .and(EventSpecification.hasCategories(categories))
                .and(EventSpecification.hasDateRange(rangeStart, rangeEnd));

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();
        Map<Long, Long> views = getViewsMap(events);

        return events.stream()
                .map(e -> EventMapper.toFullDto(e, getConfirmedRequests(e.getId()), views.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto adminUpdateEvent(Long eventId, UpdateEventAdminRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (dto.getEventDate() != null) {
            LocalDateTime newDate = dto.getEventDate();
            LocalDateTime now = LocalDateTime.now();

            if (newDate.isBefore(now)) {
                throw new ValidationException("Дата начала события не может быть в прошлом");
            }

            if (newDate.isBefore(now.plusHours(1))) {
                throw new ConflictException("Дата начала изменяемого события должна быть не ранее чем за час от даты публикации");
            }

            event.setEventDate(newDate);
        }

        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getLocation() != null) {
            event.setLat(dto.getLocation().getLat());
            event.setLon(dto.getLocation().getLon());
        }
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + dto.getCategory() + " не найдена"));
            event.setCategory(category);
        }
        if (dto.getStateAction() == StateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Можно публиковать только события в состоянии ожидания публикации");
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (dto.getStateAction() == StateAction.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Нельзя отклонить уже опубликованное событие");
            }
            event.setState(EventState.CANCELED);
        }

        Event saved = eventRepository.save(event);
        long views = getViewsForEvent(eventId);
        long confirmed = getConfirmedRequests(eventId);
        return EventMapper.toFullDto(saved, confirmed, views);
    }

    @Override
    public List<EventShortDto> searchEvents(String text, List<Long> categories, Boolean paid,
                                            String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                            String sort, Integer from, Integer size) {

        validateRange(rangeStart, rangeEnd);

        String sortValue = (sort != null && !sort.isBlank()) ? sort.toUpperCase() : "EVENT_DATE";

        if (!"EVENT_DATE".equals(sortValue) && !"VIEWS".equals(sortValue)) {
            throw new ValidationException("Unknown sort: " + sort);
        }

        Sort sorting = "VIEWS".equals(sortValue)
                ? Sort.by(Sort.Direction.DESC, "id")
                : Sort.by(Sort.Direction.DESC, "eventDate");

        Pageable pageable = PageRequest.of(from / size, size, sorting);

        Specification<Event> spec = Specification
                .where(EventSpecification.hasState(EventState.PUBLISHED))
                .and(EventSpecification.hasText(text))
                .and(EventSpecification.hasCategories(categories))
                .and(EventSpecification.hasPaid(paid))
                .and(EventSpecification.hasDateRange(rangeStart, rangeEnd));

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();
        Map<Long, Long> views = getViewsMap(events);

        return events.stream()
                .map(e -> EventMapper.toShortDto(e, getConfirmedRequests(e.getId()), views.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getPublishedEvent(Long id) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено или не опубликовано"));

        long views = getViewsForEvent(id);
        long confirmed = getConfirmedRequests(id);
        return EventMapper.toFullDto(event, confirmed, views);
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

        Map<Long, Long> views = new java.util.HashMap<>();
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

    private long getViewsForEvent(Long eventId) {
        List<ViewStats> stats = statsClient.getStats(
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now(),
                List.of("/events/" + eventId),
                true
        );
        return stats.isEmpty() ? 0L : stats.get(0).getHits();
    }

    private void validateRange(String rangeStart, String rangeEnd) {
        if (rangeStart != null && rangeEnd != null) {
            LocalDateTime start = LocalDateTime.parse(rangeStart, FORMATTER);
            LocalDateTime end = LocalDateTime.parse(rangeEnd, FORMATTER);
            if (start.isAfter(end)) {
                throw new ValidationException("rangeStart must be before rangeEnd");
            }
        }
    }
}