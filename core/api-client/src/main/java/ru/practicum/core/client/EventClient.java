package ru.practicum.core.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.core.dto.event.EventState;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/internal/events/{eventId}/exists")
    Boolean eventExists(@PathVariable("eventId") Long eventId);

    @GetMapping("/internal/events/{eventId}/state")
    EventState getEventState(@PathVariable("eventId") Long eventId);

    @GetMapping("/internal/events/{eventId}/initiator")
    Long getEventInitiator(@PathVariable("eventId") Long eventId);

    @GetMapping("/internal/events/{eventId}/participant-limit")
    Integer getEventParticipantLimit(@PathVariable("eventId") Long eventId);

    @GetMapping("/internal/events/{eventId}/request-moderation")
    Boolean getEventRequestModeration(@PathVariable("eventId") Long eventId);
}