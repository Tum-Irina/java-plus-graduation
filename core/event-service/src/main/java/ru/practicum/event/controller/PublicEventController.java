package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.service.EventService;
import ru.practicum.event.SortEvents;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.params.PublicEventsParam;

import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicEventController {
    private final EventService eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam (required = false) Long[] categories,
            @RequestParam (required = false) Boolean paid,
            @RequestParam (required = false) String rangeStart,
            @RequestParam (required = false) String rangeEnd,
            @RequestParam (defaultValue = "false") Boolean onlyAvailable,
            @RequestParam (defaultValue = "EVENT_DATE") SortEvents sort,
            @RequestParam (defaultValue = "0") Integer from,
            @RequestParam (defaultValue = "10") Integer size,
            HttpServletRequest request) {

        log.info("Получение событий через публичный эндпоинт");
        PublicEventsParam publicEventsParam = new PublicEventsParam(text, categories, paid,  rangeStart, rangeEnd, onlyAvailable, sort, from, size);
        return eventService.getEventsPublic(publicEventsParam, request.getRemoteAddr(), request.getRequestURI());
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto findById(@PathVariable Long eventId,
                                 HttpServletRequest request,
                                 @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId) {
        log.info("Получение полной информации о событии. eventId={}, userId={}", eventId, userId);

        Long actualUserId = userId != null ? userId : 0L;

        return eventService.findById(eventId, request.getRemoteAddr(), request.getRequestURI(), actualUserId);
    }

    @PutMapping("/{eventId}/like")
    @ResponseStatus(HttpStatus.OK)
    public void likeEvent(@PathVariable Long eventId,
                          @RequestHeader("X-EWM-USER-ID") Long userId) {
        log.info("PUT /events/{}/like, userId={}", eventId, userId);
        eventService.likeEvent(userId, eventId);
    }

    @GetMapping("/recommendations")
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getRecommendations(@RequestHeader("X-EWM-USER-ID") Long userId,
                                                  @RequestParam(defaultValue = "10") Integer maxResults) {
        log.info("GET /events/recommendations, userId={}, maxResults={}", userId, maxResults);
        return eventService.getRecommendations(userId, maxResults);
    }
}
