package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.State;
import ru.practicum.event.service.InternalEventService;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
@Slf4j
public class InternalEventController {

    private final InternalEventService internalEventService;

    @GetMapping("/{eventId}/exists")
    public Boolean eventExists(@PathVariable Long eventId) {
        log.info("Internal: проверка существования события {}", eventId);
        return internalEventService.eventExists(eventId);
    }

    @GetMapping("/{eventId}/state")
    public State getEventState(@PathVariable Long eventId) {
        log.info("Internal: получение статуса события {}", eventId);
        return internalEventService.getEventState(eventId);
    }

    @GetMapping("/{eventId}/initiator")
    public Long getEventInitiator(@PathVariable Long eventId) {
        log.info("Internal: получение инициатора события {}", eventId);
        return internalEventService.getEventInitiator(eventId);
    }

    @GetMapping("/{eventId}/participant-limit")
    public Integer getEventParticipantLimit(@PathVariable Long eventId) {
        log.info("Internal: получение лимита участников события {}", eventId);
        return internalEventService.getEventParticipantLimit(eventId);
    }

    @GetMapping("/{eventId}/request-moderation")
    public Boolean getEventRequestModeration(@PathVariable Long eventId) {
        log.info("Internal: получение настройки модерации события {}", eventId);
        return internalEventService.getEventRequestModeration(eventId);
    }
}