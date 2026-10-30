package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    List<Event> findAllByState(EventState state);

    Optional<Event> findByIdAndState(Long id, EventState state);

    List<Event> findAllByInitiatorId(Long initiatorId);

    long countByInitiatorId(Long initiatorId);

    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    List<Event> findAllByCategoryId(Long categoryId);

    boolean existsByCategoryId(Long categoryId);
}
