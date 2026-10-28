package ru.practicum.mapper;

import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.model.Event;

public final class EventMapper {

    private EventMapper() {
    }

    public static EventShortDto toShortDto(Event e, long views) {
        return EventShortDto.builder()
                .id(e.getId())
                .annotation(e.getAnnotation())
                .categoryId(e.getCategoryId())
                .confirmedRequests(0L)   // TODO: когда появится Request
                .eventDate(e.getEventDate())
                .initiatorId(e.getInitiatorId())
                .paid(e.getPaid())
                .title(e.getTitle())
                .views(views)
                .build();
    }

    public static EventFullDto toFullDto(Event e, long views) {
        return EventFullDto.builder()
                .id(e.getId())
                .annotation(e.getAnnotation())
                .categoryId(e.getCategoryId())
                .confirmedRequests(0L)   // TODO
                .createdOn(e.getCreatedOn())
                .description(e.getDescription())
                .eventDate(e.getEventDate())
                .initiatorId(e.getInitiatorId())
                .paid(e.getPaid())
                .participantLimit(e.getParticipantLimit())
                .publishedOn(e.getPublishedOn())
                .requestModeration(e.getRequestModeration())
                .state(e.getState().name())
                .title(e.getTitle())
                .views(views)
                .build();
    }
}