package ru.practicum.event;

import ru.practicum.event.dto.*;
import ru.practicum.event.params.AdminEventsParam;
import ru.practicum.event.params.PublicEventsParam;

import java.util.List;

public interface EventService {

    EventFullDto createEvent(NewEventDto newEventDto, Long initiatorId);

    EventFullDto updateEventUser(UpdateEventUserRequest updateEventUserRequest, Long initiatorId, Long eventId);

    EventFullDto updateEventByAdmin(UpdateEventAdminRequest updateEventAdminRequest, Long eventId);

    List<EventShortDto> findByInitiatorId(Long initiatorId, int from, int size);

    List<EventShortDto> getEventsPublic(PublicEventsParam publicEventsParam, String ip, String uri);

    List<EventFullDto> searchEventsByAdmin(AdminEventsParam adminEventsParam);

    EventFullDto findById(Long eventId, String ip, String uri);

    EventFullDto findByIdAndInitiatorId(Long initiatorId, Long eventId);
}
