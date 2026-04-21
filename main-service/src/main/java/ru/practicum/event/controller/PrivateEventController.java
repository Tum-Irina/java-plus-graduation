package ru.practicum.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.EventService;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PrivateEventController {
    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@Valid @RequestBody NewEventDto newEventDto,
                                    @PathVariable("userId") Long initiatorId) {
        log.info("POST-запрос на создание события: {}", newEventDto);

        return eventService.createEvent(newEventDto, initiatorId);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@Valid @RequestBody UpdateEventUserRequest updateEventUserRequest,
                                    @PathVariable("userId") Long initiatorId,
                                    @PathVariable Long eventId) {
        log.info("PATCH запрос на обновление события с id: , добавленного текущим пользователем {}", eventId);
        return eventService.updateEventUser(updateEventUserRequest, initiatorId, eventId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> findByInitiatorId(@PathVariable("userId") Long initiatorId,
                                                 @RequestParam(defaultValue = "0") int from,
                                                 @RequestParam (defaultValue = "10") int size) {
        log.info("Получение событий, добавленных текущим пользователем");
        return eventService.findByInitiatorId(initiatorId, from, size);
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto findByIdAndInitiatorId(@PathVariable("userId") Long initiatorId,
                                               @PathVariable Long eventId) {
        log.info("Получение подробной информации о событии, добавленном текущим пользователем");
        return eventService.findByIdAndInitiatorId(initiatorId, eventId);
    }
}
