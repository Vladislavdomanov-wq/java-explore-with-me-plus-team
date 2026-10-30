package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Optional<Event> findByIdAndState(Long id, EventState state);

    Page<Event> findByInitiatorId(Long userId, Pageable pageable);

    Page<Event> findByState(EventState state, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);
}