package ru.practicum.core.request.service;

import ru.practicum.core.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {
    // Добавление запроса на участие в событии
    ParticipationRequestDto addRequest(Long userId, Long eventId);

    // Отмена запроса на участие в событии
    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    // Получение списка запросов текущего пользователя на участие в чужих событиях
    List<ParticipationRequestDto> getUserRequests(Long userId);
}
