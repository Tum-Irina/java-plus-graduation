package ru.practicum.core.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.core.dto.request.ParticipationRequestDto;
import ru.practicum.core.request.service.InternalRequestService;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
@Slf4j
public class InternalRequestController {

    private final InternalRequestService internalRequestService;

    @GetMapping("/event/{eventId}")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long eventId) {
        log.info("Internal GET /internal/requests/event/{}", eventId);
        return internalRequestService.getEventRequests(eventId);
    }

    @GetMapping("/event/{eventId}/confirmed/count")
    public Long getConfirmedRequestsCount(@PathVariable Long eventId) {
        log.info("Internal GET /internal/requests/event/{}/confirmed/count", eventId);
        return internalRequestService.getConfirmedRequestsCount(eventId);
    }

    @GetMapping("/events/confirmed/count")
    public Map<Long, Long> getConfirmedRequestsCounts(@RequestParam Set<Long> eventIds) {
        log.info("Internal GET /internal/requests/events/confirmed/count?eventIds={}", eventIds);
        return internalRequestService.getConfirmedRequestsCounts(eventIds);
    }

    @GetMapping("/user/{userId}/event/{eventId}")
    public ParticipationRequestDto getUserRequest(@PathVariable Long userId,
                                                  @PathVariable Long eventId) {
        log.info("Internal GET /internal/requests/user/{}/event/{}", userId, eventId);
        return internalRequestService.getUserRequest(userId, eventId);
    }

    @GetMapping("/user/{userId}/event/{eventId}/exists/confirmed")
    public Boolean hasConfirmedRequest(@PathVariable Long userId,
                                       @PathVariable Long eventId) {
        log.info("Internal GET /internal/requests/user/{}/event/{}/exists/confirmed", userId, eventId);
        return internalRequestService.hasConfirmedRequest(userId, eventId);
    }
}