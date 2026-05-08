package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.State;
import ru.practicum.event.repository.EventRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InternalEventService {

    private final EventRepository eventRepository;

    public Boolean eventExists(Long eventId) {
        log.debug("Internal: проверка существования события {}", eventId);
        return eventRepository.existsById(eventId);
    }

    public State getEventState(Long eventId) {
        log.debug("Internal: получение статуса события {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Событие не найдено"))
                .getState();
    }

    public Long getEventInitiator(Long eventId) {
        log.debug("Internal: получение инициатора события {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Событие не найдено"))
                .getInitiatorId();
    }

    public Integer getEventParticipantLimit(Long eventId) {
        log.debug("Internal: получение лимита участников события {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Событие не найдено"))
                .getParticipantLimit();
    }

    public Boolean getEventRequestModeration(Long eventId) {
        log.debug("Internal: получение настройки модерации события {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Событие не найдено"))
                .getRequestModeration();
    }
}