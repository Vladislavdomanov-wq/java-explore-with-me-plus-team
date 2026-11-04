package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.Subscription;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriberIdAndAuthorId(Long subscriberId, Long authorId);

    boolean existsBySubscriberIdAndAuthorId(Long subscriberId, Long authorId);

    List<Subscription> findAllBySubscriberId(Long subscriberId);

    List<Subscription> findAllByAuthorId(Long authorId);

    long countByAuthorId(Long authorId);

    long countBySubscriberId(Long subscriberId);
}