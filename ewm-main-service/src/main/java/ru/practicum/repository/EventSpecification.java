package ru.practicum.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> hasText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return cb.conjunction();
            }
            String lowerText = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("annotation")), lowerText),
                    cb.like(cb.lower(root.get("description")), lowerText)
            );
        };
    }

    public static Specification<Event> hasCategories(List<Long> categories) {
        return (root, query, cb) -> {
            if (categories == null || categories.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("category").get("id").in(categories);
        };
    }

    public static Specification<Event> hasPaid(Boolean paid) {
        return (root, query, cb) -> {
            if (paid == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("paid"), paid);
        };
    }

    public static Specification<Event> hasDateRange(String rangeStart, String rangeEnd) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (rangeStart != null && !rangeStart.isBlank()) {
                LocalDateTime start = LocalDateTime.parse(rangeStart,
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), start));
            } else {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), LocalDateTime.now()));
            }

            if (rangeEnd != null && !rangeEnd.isBlank()) {
                LocalDateTime end = LocalDateTime.parse(rangeEnd,
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), end));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Event> hasState(EventState state) {
        return (root, query, cb) -> {
            if (state == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("state"), state);
        };
    }

    public static Specification<Event> hasStates(List<String> states) {
        return (root, query, cb) -> {
            if (states == null || states.isEmpty()) {
                return cb.conjunction();
            }
            List<EventState> eventStates = states.stream()
                    .map(EventState::valueOf)
                    .toList();
            return root.get("state").in(eventStates);
        };
    }

    public static Specification<Event> hasInitiators(List<Long> users) {
        return (root, query, cb) -> {
            if (users == null || users.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("initiator").get("id").in(users);
        };
    }
}