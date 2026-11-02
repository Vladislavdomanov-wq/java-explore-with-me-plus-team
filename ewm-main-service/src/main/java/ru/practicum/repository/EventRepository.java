package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByState(EventState state);

    Optional<Event> findByIdAndState(Long id, EventState state);

    @Query("SELECT e FROM Event e " +
            "WHERE e.state = :state " +
            "AND e.initiatorId IN (" +
            "   SELECT s.author.id FROM Subscription s WHERE s.subscriber.id = :subscriberId" +
            ") " +
            "ORDER BY e.eventDate")
    List<Event> findPublishedEventsOfFollowedAuthors(@Param("subscriberId") Long subscriberId,
                                                     @Param("state") EventState state);
}