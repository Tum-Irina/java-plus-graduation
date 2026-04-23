package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.EventService;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.event.State;
import ru.practicum.event.dto.*;
import ru.practicum.event.params.AdminEventsParam;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminEventController {

    private final EventService eventService;

    // Редактирование данных события и его статуса администратором
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> searchEventsByAdmin(
            @RequestParam(required = false) Long[] users,
            @RequestParam(required = false) State[] states,
            @RequestParam (required = false) Long[] categories,
            @RequestParam (required = false) Boolean paid,
            @RequestParam (required = false) String rangeStart,
            @RequestParam (required = false) String rangeEnd,
            @RequestParam (defaultValue = "0") Integer from,
            @RequestParam (defaultValue = "10") Integer size,
            HttpServletRequest request) {

        log.info("Поиск событий администратором");
        AdminEventsParam adminEventsParam = new AdminEventsParam(users, states, categories,  rangeStart, rangeEnd, from, size);
        return eventService.searchEventsByAdmin(adminEventsParam);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByAdmin(@PathVariable Long eventId,
                                           @Valid @RequestBody UpdateEventAdminRequest request) {
        log.info("PATCH /admin/events/{} - администратор редактирует событие", eventId);
        return eventService.updateEventByAdmin(request, eventId);
    }
}
