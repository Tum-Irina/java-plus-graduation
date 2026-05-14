package ru.practicum.core.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.client.EventClient;
import ru.practicum.core.client.UserClient;
import ru.practicum.core.dto.event.EventState;
import ru.practicum.core.dto.request.ParticipationRequestDto;
import ru.practicum.core.exception.ConflictException;
import ru.practicum.core.exception.NotFoundException;
import ru.practicum.core.request.mapper.RequestMapper;
import ru.practicum.core.request.model.Request;
import ru.practicum.core.request.model.RequestStatus;
import ru.practicum.core.request.repository.RequestRepository;
import ru.practicum.stats.client.CollectorClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final CollectorClient collectorClient;

    // Добавление запроса на участие в событии
    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        log.info("Пользователь userId = {} создает запрос на участие в событии eventId = {}", userId, eventId);

        // Проверка на добавление повторного запроса
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            log.warn("Попытка повторного запроса: userId = {}, eventId = {}", userId, eventId);
            throw new ConflictException("Нельзя добавить повторный запрос.");
        }

        // Проверка существования пользователя и события, валидация события
        findUserById(userId);
        findEventById(eventId);
        validateEventForRequest(eventId, userId);

        Request request = Request.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
                .status(RequestStatus.PENDING)
                .build();

        Integer participantLimit = eventClient.getEventParticipantLimit(eventId);
        Boolean requestModeration = eventClient.getEventRequestModeration(eventId);

        if (!requestModeration || participantLimit == 0) {
            log.info("Заявка подтверждена автоматически (лимит = 0 или модерация отключена): eventId = {}", eventId);
            request.setStatus(RequestStatus.CONFIRMED);
        }

        Request savedRequest = requestRepository.save(request);
        log.info("Запрос успешно создан: requestId = {}, status = {}", savedRequest.getId(), savedRequest.getStatus());

        collectorClient.sendRegisterEvent(userId, eventId, Instant.now());

        return RequestMapper.toDto(savedRequest);
    }

    // Отмена запроса на участие в событии
    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Пользователь userId = {} отменяет запрос requestId = {}", userId, requestId);

        // Проверка существования пользователя
        findUserById(userId);

        // Проверка и получение запроса
        Request request = findRequestAndCheckOwner(requestId, userId);
        request.setStatus(RequestStatus.CANCELED);

        Request savedRequest = requestRepository.save(request);
        log.info("Запрос requestId = {} переведен в статус CANCELED", requestId);

        return RequestMapper.toDto(savedRequest);
    }

    // Получение списка запросов текущего пользователя на участие в чужих событиях
    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение всех запросов пользователя userId = {}", userId);

        // Проверка существования пользователя
        findUserById(userId);

        List<ParticipationRequestDto> requests = requestRepository.findAllByRequesterId(userId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());

        log.info("Найдено {} запросов для userId = {}", requests.size(), userId);

        return requests;
    }

    // Проверка существования пользователя
    private void findUserById(Long userId) {
        if (!userClient.userExists(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.");
        }
    }

    // Проверка существования события
    private void findEventById(Long eventId) {
        if (!eventClient.eventExists(eventId)) {
            throw new NotFoundException("Событие с id = " + eventId + " не найдено.");
        }
    }

    // Валидация события для запроса
    private void validateEventForRequest(Long eventId, Long userId) {
        Long initiatorId = eventClient.getEventInitiator(eventId);
        if (initiatorId.equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии.");
        }

        EventState state = eventClient.getEventState(eventId);
        if (state != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии.");
        }

        Integer participantLimit = eventClient.getEventParticipantLimit(eventId);
        if (participantLimit > 0) {
            Long confirmedRequests = requestRepository
                    .countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            if (confirmedRequests >= participantLimit) {
                throw new ConflictException("У события достигнут лимит запросов на участие.");
            }
        }
    }

    // Проверка существования запроса и проверка запроса пользователя
    private Request findRequestAndCheckOwner(Long requestId, Long userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id = " + requestId + " не найден."));

        if (!Objects.equals(request.getRequesterId(), userId)) {
            throw new NotFoundException("Запрос с id = " + requestId + " не найден у текущего пользователя.");
        }
        return request;
    }
}