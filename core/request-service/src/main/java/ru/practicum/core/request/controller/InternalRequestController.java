package ru.practicum.core.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.core.dto.request.ParticipationRequestDto;
import ru.practicum.core.request.model.RequestStatus;
import ru.practicum.core.request.repository.RequestRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
@Slf4j
public class InternalRequestController {

    private final RequestRepository requestRepository;

    @GetMapping("/event/{eventId}")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long eventId) {
        log.info("Internal GET /internal/requests/event/{}", eventId);
        return requestRepository.findAllByEventId(eventId).stream()
                .map(request -> ParticipationRequestDto.builder()
                        .id(request.getId())
                        .event(request.getEventId())
                        .requester(request.getRequesterId())
                        .created(request.getCreated().toString())
                        .status(request.getStatus().toString())
                        .build())
                .collect(Collectors.toList());
    }

    @GetMapping("/event/{eventId}/confirmed/count")
    public Long getConfirmedRequestsCount(@PathVariable Long eventId) {
        log.info("Internal GET /internal/requests/event/{}/confirmed/count", eventId);
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    @GetMapping("/events/confirmed/count")
    public Map<Long, Long> getConfirmedRequestsCounts(@RequestParam Set<Long> eventIds) {
        log.info("Internal GET /internal/requests/events/confirmed/count?eventIds={}", eventIds);
        List<Object[]> results = requestRepository.countConfirmedRequestsByEventIds(eventIds);
        return results.stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (Long) result[1]
                ));
    }

    @GetMapping("/user/{userId}/event/{eventId}")
    public ParticipationRequestDto getUserRequest(@PathVariable Long userId,
                                                  @PathVariable Long eventId) {
        log.info("Internal GET /internal/requests/user/{}/event/{}", userId, eventId);
        return requestRepository.findAllByEventId(eventId).stream()
                .filter(r -> r.getRequesterId().equals(userId))
                .findFirst()
                .map(request -> ParticipationRequestDto.builder()
                        .id(request.getId())
                        .event(request.getEventId())
                        .requester(request.getRequesterId())
                        .created(request.getCreated().toString())
                        .status(request.getStatus().toString())
                        .build())
                .orElse(null);
    }

    @GetMapping("/user/{userId}/event/{eventId}/exists/confirmed")
    public Boolean hasConfirmedRequest(@PathVariable Long userId,
                                       @PathVariable Long eventId) {
        log.info("Internal GET /internal/requests/user/{}/event/{}/exists/confirmed", userId, eventId);
        return requestRepository.existsByRequesterIdAndEventIdAndStatus(
                userId, eventId, RequestStatus.CONFIRMED
        );
    }
}