package ru.practicum.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.LocationDto;
import ru.practicum.dto.NewEventDto;
import ru.practicum.dto.UpdateEventAdminRequest;
import ru.practicum.dto.UpdateEventUserRequest;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;
import ru.practicum.model.RequestStatus;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.RequestRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final int DEFAULT_FROM = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final String SORT_EVENT_DATE = "EVENT_DATE";
    private static final String SORT_VIEWS = "VIEWS";

    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventDtoAssembler eventDtoAssembler;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto request) {
        checkUser(userId);
        checkCategory(request.getCategory());
        checkEventDateForUser(request.getEventDate());

        Event event = Event.builder()
                .annotation(request.getAnnotation())
                .categoryId(request.getCategory())
                .createdOn(LocalDateTime.now())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .initiatorId(userId)
                .lat(request.getLocation().getLat())
                .lon(request.getLocation().getLon())
                .paid(Boolean.TRUE.equals(request.getPaid()))
                .participantLimit(request.getParticipantLimit() == null ? 0 : request.getParticipantLimit())
                .requestModeration(request.getRequestModeration() == null || request.getRequestModeration())
                .state(EventState.PENDING)
                .title(request.getTitle())
                .build();

        return eventDtoAssembler.toFull(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        checkUser(userId);
        List<Event> events = eventRepository.findAllByInitiatorId(userId).stream()
                .sorted(Comparator.comparing(Event::getEventDate))
                .toList();
        return paginate(eventDtoAssembler.toShortList(events), from, size);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        checkUser(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return eventDtoAssembler.toFull(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        checkUser(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        applyCommonUpdates(event, request.getAnnotation(), request.getCategory(), request.getDescription(),
                request.getEventDate(), request.getLocation(), request.getPaid(),
                request.getParticipantLimit(), request.getRequestModeration(), request.getTitle(), true);

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case "SEND_TO_REVIEW" -> event.setState(EventState.PENDING);
                case "CANCEL_REVIEW" -> event.setState(EventState.CANCELED);
                default -> throw new ValidationException("Unknown state action: " + request.getStateAction());
            }
        }

        return eventDtoAssembler.toFull(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        applyCommonUpdates(event, request.getAnnotation(), request.getCategory(), request.getDescription(),
                request.getEventDate(), request.getLocation(), request.getPaid(),
                request.getParticipantLimit(), request.getRequestModeration(), request.getTitle(), false);

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case "PUBLISH_EVENT" -> {
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Event must be in PENDING state to be published");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                }
                case "REJECT_EVENT" -> {
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Published event cannot be rejected");
                    }
                    event.setState(EventState.CANCELED);
                }
                default -> throw new ValidationException("Unknown state action: " + request.getStateAction());
            }
        }

        return eventDtoAssembler.toFull(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEvents(List<Long> users, List<String> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                             Integer from, Integer size) {
        validateRange(rangeStart, rangeEnd);
        List<EventState> eventStates = parseStates(states);

        Specification<Event> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (users != null && !users.isEmpty()) {
                predicates.add(root.get("initiatorId").in(users));
            }
            if (eventStates != null && !eventStates.isEmpty()) {
                predicates.add(root.get("state").in(eventStates));
            }
            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("categoryId").in(categories));
            }
            if (rangeStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("eventDate"), rangeStart));
            }
            if (rangeEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("eventDate"), rangeEnd));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Event> events = eventRepository.findAll(specification).stream()
                .sorted(Comparator.comparing(Event::getId))
                .toList();
        return paginate(eventDtoAssembler.toFullList(events), from, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getPublishedEvents(String text, List<Long> categories, Boolean paid,
                                                  LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                  Boolean onlyAvailable, String sort,
                                                  Integer from, Integer size) {
        validateRange(rangeStart, rangeEnd);
        String sortValue = sort == null || sort.isBlank() ? SORT_EVENT_DATE : sort;
        if (!SORT_EVENT_DATE.equals(sortValue) && !SORT_VIEWS.equals(sortValue)) {
            throw new ValidationException("Unknown sort: " + sort);
        }

        Specification<Event> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));
            if (text != null && !text.isBlank()) {
                String pattern = "%" + text.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("annotation")), pattern),
                        cb.like(cb.lower(root.<String>get("description")), pattern)
                ));
            }
            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("categoryId").in(categories));
            }
            if (paid != null) {
                predicates.add(cb.equal(root.get("paid"), paid));
            }
            if (rangeStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("eventDate"), rangeStart));
            }
            if (rangeEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("eventDate"), rangeEnd));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Event> events = eventRepository.findAll(specification).stream()
                .sorted(Comparator.comparing(Event::getEventDate))
                .toList();

        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = events.stream()
                    .filter(this::hasFreeSlots)
                    .toList();
        }

        List<EventShortDto> result = eventDtoAssembler.toShortList(events);
        if (SORT_VIEWS.equals(sortValue)) {
            result = result.stream()
                    .sorted(Comparator.comparing(EventShortDto::getViews,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
        return paginate(result, from, size);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublishedEvent(Long id) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event not found: " + id));
        return eventDtoAssembler.toFull(event);
    }

    private void applyCommonUpdates(Event event, String annotation, Long categoryId, String description,
                                    LocalDateTime eventDate, LocationDto location, Boolean paid,
                                    Integer participantLimit, Boolean requestModeration, String title,
                                    boolean isUserRequest) {
        if (annotation != null) {
            event.setAnnotation(annotation);
        }
        if (categoryId != null) {
            checkCategory(categoryId);
            event.setCategoryId(categoryId);
        }
        if (description != null) {
            event.setDescription(description);
        }
        if (eventDate != null) {
            if (isUserRequest) {
                checkEventDateForUser(eventDate);
            } else if (eventDate.isBefore(LocalDateTime.now())) {
                throw new ValidationException("Event date cannot be in the past");
            }
            event.setEventDate(eventDate);
        }
        if (location != null) {
            event.setLat(location.getLat());
            event.setLon(location.getLon());
        }
        if (paid != null) {
            event.setPaid(paid);
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
        }
        if (requestModeration != null) {
            event.setRequestModeration(requestModeration);
        }
        if (title != null) {
            event.setTitle(title);
        }
    }

    private boolean hasFreeSlots(Event event) {
        int limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        if (limit == 0) {
            return true;
        }
        long confirmed = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        return confirmed < limit;
    }

    private void checkUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private void checkCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Category with id=" + categoryId + " was not found");
        }
    }

    private void checkEventDateForUser(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least 2 hours from now");
        }
    }

    private void validateRange(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("rangeStart must be before rangeEnd");
        }
    }

    private List<EventState> parseStates(List<String> states) {
        if (states == null || states.isEmpty()) {
            return List.of();
        }
        List<EventState> result = new ArrayList<>();
        for (String state : states) {
            try {
                result.add(EventState.valueOf(state));
            } catch (IllegalArgumentException ex) {
                throw new ValidationException("Unknown state: " + state);
            }
        }
        return result;
    }

    private <T> List<T> paginate(List<T> items, Integer from, Integer size) {
        int offset = from == null ? DEFAULT_FROM : from;
        int limit = size == null ? DEFAULT_SIZE : size;
        if (offset < 0 || limit < 0) {
            throw new ValidationException("from and size must be non-negative");
        }
        if (offset >= items.size() || limit == 0) {
            return List.of();
        }
        return items.subList(offset, Math.min(offset + limit, items.size()));
    }
}
