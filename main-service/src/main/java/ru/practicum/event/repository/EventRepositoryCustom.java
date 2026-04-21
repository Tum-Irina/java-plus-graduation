package ru.practicum.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.event.Event;
import ru.practicum.event.params.AdminEventsParam;
import ru.practicum.event.params.PublicEventsParam;

public interface EventRepositoryCustom {

    Page<Event> getEventsPublic(PublicEventsParam publicEventsParam, Pageable pageable);

    Page<Event> searchEventsByAdmin(AdminEventsParam adminEventsParam, Pageable pageable);
}
