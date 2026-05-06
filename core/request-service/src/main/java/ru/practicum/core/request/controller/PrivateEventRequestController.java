package ru.practicum.core.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.core.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.core.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.core.dto.request.ParticipationRequestDto;
import ru.practicum.core.request.service.RequestEventService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
@Slf4j
public class PrivateEventRequestController {

    private final RequestEventService requestEventService;

    @GetMapping
    public List<ParticipationRequestDto> getEventParticipants(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/requests - получение запросов на участие в событии", userId, eventId);
        return requestEventService.getEventParticipants(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResult changeRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest request) {
        log.info("PATCH /users/{}/events/{}/requests - изменение статуса запросов", userId, eventId);
        return requestEventService.changeRequestStatus(userId, eventId, request);
    }
}