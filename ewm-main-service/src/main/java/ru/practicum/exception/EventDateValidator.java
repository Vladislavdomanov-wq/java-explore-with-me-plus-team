package ru.practicum.exception;

import java.time.LocalDateTime;

public class EventDateValidator {

    public static void validateEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException(
                    "Дата события должна быть не ранее чем через 2 часа от текущего момента"
            );
        }
    }
}