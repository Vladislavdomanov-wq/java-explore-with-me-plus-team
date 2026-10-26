package ru.practicum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventShortDto {
    private Long id;
    private String annotation;
    private Long categoryId;
    private Long confirmedRequests;
    private LocalDateTime eventDate;
    private Long initiatorId;
    private Boolean paid;
    private String title;
    private Long views;
}