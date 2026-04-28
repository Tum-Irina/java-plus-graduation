package ru.practicum.request.service;

import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestEventService {
    // Получение информации о запросах на участие в событии текущего пользователя
    List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId);

    // Изменение статуса заявок на участие в событии текущего пользователя
    EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest updateRequest);
}