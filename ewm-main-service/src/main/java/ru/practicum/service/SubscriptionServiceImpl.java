package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.SubscriptionDto;
import ru.practicum.dto.SubscriptionStatsDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.SubscriptionMapper;
import ru.practicum.model.Subscription;
import ru.practicum.model.User;
import ru.practicum.repository.SubscriptionRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SubscriptionDto subscribe(Long subscriberId, Long authorId) {
        if (subscriberId.equals(authorId)) {
            throw new ValidationException("User cannot subscribe to himself");
        }
        User subscriber = userRepository.findById(subscriberId)
                .orElseThrow(() -> new NotFoundException("Subscriber not found: " + subscriberId));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Author not found: " + authorId));

        if (subscriptionRepository.existsBySubscriberIdAndAuthorId(subscriberId, authorId)) {
            throw new ConflictException("Subscription already exists");
        }

        Subscription subscription = Subscription.builder()
                .subscriber(subscriber)
                .author(author)
                .createdOn(LocalDateTime.now())
                .build();

        return SubscriptionMapper.toDto(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional
    public void unsubscribe(Long subscriberId, Long authorId) {
        Subscription subscription = subscriptionRepository
                .findBySubscriberIdAndAuthorId(subscriberId, authorId)
                .orElseThrow(() -> new NotFoundException(
                        "Subscription not found: subscriber=" + subscriberId + ", author=" + authorId));
        subscriptionRepository.delete(subscription);
    }

    @Override
    public List<SubscriptionDto> getSubscriptions(Long userId) {
        return subscriptionRepository.findAllBySubscriberId(userId).stream()
                .map(SubscriptionMapper::toDto)
                .toList();
    }

    @Override
    public List<SubscriptionDto> getSubscribers(Long userId) {
        return subscriptionRepository.findAllByAuthorId(userId).stream()
                .map(SubscriptionMapper::toDto)
                .toList();
    }

    @Override
    public SubscriptionStatsDto getStats(Long userId) {
        return SubscriptionStatsDto.builder()
                .userId(userId)
                .followersCount(subscriptionRepository.countByAuthorId(userId))
                .followingCount(subscriptionRepository.countBySubscriberId(userId))
                .build();
    }
}