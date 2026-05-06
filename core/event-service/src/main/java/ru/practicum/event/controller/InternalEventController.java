package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.State;
import ru.practicum.event.repository.EventRepository;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
@Slf4j
public class InternalEventController {

    private final EventRepository eventRepository;

    @GetMapping("/{eventId}/exists")
    public Boolean eventExists(@PathVariable Long eventId) {
        log.info("Internal: проверка существования события {}", eventId);
        return eventRepository.existsById(eventId);
    }

    @GetMapping("/{eventId}/state")
    public State getEventState(@PathVariable Long eventId) {
        log.info("Internal: получение статуса события {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Событие не найдено"))
                .getState();
    }

    @GetMapping("/{eventId}/initiator")
    public Long getEventInitiator(@PathVariable Long eventId) {
        log.info("Internal: получение инициатора события {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Событие не найдено"))
                .getInitiatorId();
    }

    @GetMapping("/{eventId}/participant-limit")
    public Integer getEventParticipantLimit(@PathVariable Long eventId) {
        log.info("Internal: получение лимита участников события {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Событие не найдено"))
                .getParticipantLimit();
    }

    @GetMapping("/{eventId}/request-moderation")
    public Boolean getEventRequestModeration(@PathVariable Long eventId) {
        log.info("Internal: получение настройки модерации события {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Событие не найдено"))
                .getRequestModeration();
    }
}