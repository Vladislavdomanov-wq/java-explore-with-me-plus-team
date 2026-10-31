package ru.practicum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationDto {

    @NotNull(message = "Широта обязательна")
    private Float lat;

    @NotNull(message = "Долгота обязательна")
    private Float lon;
}
