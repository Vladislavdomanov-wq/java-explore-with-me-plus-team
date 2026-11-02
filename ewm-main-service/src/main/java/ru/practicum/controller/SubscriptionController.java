package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.SubscriptionDto;
import ru.practicum.dto.SubscriptionStatsDto;
import ru.practicum.service.SubscriptionService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{authorId}")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionDto subscribe(@PathVariable Long userId,
                                     @PathVariable Long authorId) {
        return subscriptionService.subscribe(userId, authorId);
    }

    @DeleteMapping("/{authorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@PathVariable Long userId,
                            @PathVariable Long authorId) {
        subscriptionService.unsubscribe(userId, authorId);
    }

    @GetMapping
    public List<SubscriptionDto> getSubscriptions(@PathVariable Long userId) {
        return subscriptionService.getSubscriptions(userId);
    }

    @GetMapping("/subscribers")
    public List<SubscriptionDto> getSubscribers(@PathVariable Long userId) {
        return subscriptionService.getSubscribers(userId);
    }

    @GetMapping("/stats")
    public SubscriptionStatsDto getStats(@PathVariable Long userId) {
        return subscriptionService.getStats(userId);
    }
}