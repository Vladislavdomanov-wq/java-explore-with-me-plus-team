package ru.practicum.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StateAction {
    PUBLISH_EVENT,
    REJECT_EVENT,
    SEND_TO_REVIEW,
    CANCEL_REVIEW;

    @JsonCreator
    public static StateAction fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return StateAction.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Недопустимое значение stateAction: " + value);
        }
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}