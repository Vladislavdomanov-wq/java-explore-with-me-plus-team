package ru.practicum.mapper;

import ru.practicum.dto.SubscriptionDto;
import ru.practicum.model.Subscription;

public final class SubscriptionMapper {

    private SubscriptionMapper() {
    }

    public static SubscriptionDto toDto(Subscription s) {
        return SubscriptionDto.builder()
                .id(s.getId())
                .subscriberId(s.getSubscriber().getId())
                .authorId(s.getAuthor().getId())
                .createdOn(s.getCreatedOn())
                .build();
    }
}