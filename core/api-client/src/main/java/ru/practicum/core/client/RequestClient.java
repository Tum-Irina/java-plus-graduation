package ru.practicum.core.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.core.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.core.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.core.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/internal/requests/event/{eventId}")
    List<ParticipationRequestDto> getEventRequests(@PathVariable("eventId") Long eventId);

    @PatchMapping("/internal/requests/event/{eventId}")
    EventRequestStatusUpdateResult updateRequestStatus(@PathVariable("eventId") Long eventId,
                                                       @RequestBody EventRequestStatusUpdateRequest request);

    @GetMapping("/internal/requests/user/{userId}/event/{eventId}")
    ParticipationRequestDto getUserRequest(@PathVariable("userId") Long userId,
                                           @PathVariable("eventId") Long eventId);

    @PostMapping("/internal/requests")
    ParticipationRequestDto createRequest(@RequestParam("userId") Long userId,
                                          @RequestParam("eventId") Long eventId);

    @PatchMapping("/internal/requests/{requestId}/cancel")
    ParticipationRequestDto cancelRequest(@RequestParam("userId") Long userId,
                                          @PathVariable("requestId") Long requestId);

    @GetMapping("/internal/requests/user/{userId}")
    List<ParticipationRequestDto> getUserRequests(@PathVariable("userId") Long userId);

    @GetMapping("/internal/requests/event/{eventId}/confirmed/count")
    Long getConfirmedRequestsCount(@PathVariable("eventId") Long eventId);

    @GetMapping("/internal/requests/events/confirmed/count")
    Map<Long, Long> getConfirmedRequestsCounts(@RequestParam("eventIds") Set<Long> eventIds);

    @GetMapping("/internal/requests/user/{userId}/event/{eventId}/exists/confirmed")
    Boolean hasConfirmedRequest(@PathVariable("userId") Long userId,
                                @PathVariable("eventId") Long eventId);
}