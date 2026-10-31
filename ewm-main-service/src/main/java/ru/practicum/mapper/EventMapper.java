package ru.practicum.mapper;

import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.LocationDto;
import ru.practicum.dto.UserShortDto;
import ru.practicum.model.Event;

public final class EventMapper {

    private EventMapper() {
    }

    public static EventShortDto toShortDto(Event e, CategoryDto category, UserShortDto initiator,
                                           long confirmedRequests, long views) {
        return EventShortDto.builder()
                .id(e.getId())
                .annotation(e.getAnnotation())
                .category(category)
                .confirmedRequests(confirmedRequests)
                .eventDate(e.getEventDate())
                .initiator(initiator)
                .paid(e.getPaid())
                .title(e.getTitle())
                .views(views)
                .build();
    }

    public static EventFullDto toFullDto(Event e, CategoryDto category, UserShortDto initiator,
                                         long confirmedRequests, long views) {
        return EventFullDto.builder()
                .id(e.getId())
                .annotation(e.getAnnotation())
                .category(category)
                .confirmedRequests(confirmedRequests)
                .createdOn(e.getCreatedOn())
                .description(e.getDescription())
                .eventDate(e.getEventDate())
                .initiator(initiator)
                .location(LocationDto.builder().lat(e.getLat()).lon(e.getLon()).build())
                .paid(e.getPaid())
                .participantLimit(e.getParticipantLimit())
                .publishedOn(e.getPublishedOn())
                .requestModeration(e.getRequestModeration())
                .state(e.getState().name())
                .title(e.getTitle())
                .views(views)
                .build();
    }

    public static EventFullDto toFullDto(Event event, long confirmedRequests, long views) {
        return toFullDto(
                event,
                new CategoryDto(event.getCategory().getId(), event.getCategory().getName()),
                new UserShortDto(event.getInitiator().getId(), event.getInitiator().getName()),
                confirmedRequests,
                views
        );
    }

    public static EventShortDto toShortDto(Event event, long confirmedRequests, long views) {
        return toShortDto(
                event,
                new CategoryDto(event.getCategory().getId(), event.getCategory().getName()),
                new UserShortDto(event.getInitiator().getId(), event.getInitiator().getName()),
                confirmedRequests,
                views
        );
    }
}
