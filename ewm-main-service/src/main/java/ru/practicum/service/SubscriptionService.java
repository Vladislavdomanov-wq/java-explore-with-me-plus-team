package ru.practicum.service;

import ru.practicum.dto.SubscriptionDto;
import ru.practicum.dto.SubscriptionStatsDto;

import java.util.List;

public interface SubscriptionService {

    SubscriptionDto subscribe(Long subscriberId, Long authorId);

    void unsubscribe(Long subscriberId, Long authorId);

    List<SubscriptionDto> getSubscriptions(Long userId);

    List<SubscriptionDto> getSubscribers(Long userId);

    SubscriptionStatsDto getStats(Long userId);
}