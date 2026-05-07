package ru.practicum.core.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.dto.request.ParticipationRequestDto;
import ru.practicum.core.request.model.RequestStatus;
import ru.practicum.core.request.repository.RequestRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InternalRequestService {

    private final RequestRepository requestRepository;

    public List<ParticipationRequestDto> getEventRequests(Long eventId) {
        log.debug("Internal GET /internal/requests/event/{}", eventId);
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

    public Long getConfirmedRequestsCount(Long eventId) {
        log.debug("Internal GET /internal/requests/event/{}/confirmed/count", eventId);
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    public Map<Long, Long> getConfirmedRequestsCounts(Set<Long> eventIds) {
        log.debug("Internal GET /internal/requests/events/confirmed/count?eventIds={}", eventIds);
        List<Object[]> results = requestRepository.countConfirmedRequestsByEventIds(eventIds);
        return results.stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (Long) result[1]
                ));
    }

    public ParticipationRequestDto getUserRequest(Long userId, Long eventId) {
        log.debug("Internal GET /internal/requests/user/{}/event/{}", userId, eventId);
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

    public Boolean hasConfirmedRequest(Long userId, Long eventId) {
        log.debug("Internal GET /internal/requests/user/{}/event/{}/exists/confirmed", userId, eventId);
        return requestRepository.existsByRequesterIdAndEventIdAndStatus(
                userId, eventId, RequestStatus.CONFIRMED
        );
    }
}