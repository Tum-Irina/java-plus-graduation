package ru.practicum.core.request.service;

import ru.practicum.core.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.core.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.core.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestEventService {
    // Получение информации о запросах на участие в событии текущего пользователя
    List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId);

    // Изменение статуса заявок на участие в событии текущего пользователя
    EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest updateRequest);
}