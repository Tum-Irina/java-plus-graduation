package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.Event;
import ru.practicum.event.State;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

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
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

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
        User user = findUserById(userId);
        Event event = findEventById(eventId);
        validateEventForRequest(event, userId);

        Request request = Request.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(user)
                .status(RequestStatus.PENDING)
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            log.info("Заявка подтверждена автоматически (лимит = 0 или модерация отключена): eventId = {}", eventId);
            request.setStatus(RequestStatus.CONFIRMED);
        }

        Request savedRequest = requestRepository.save(request);
        log.info("Запрос успешно создан: requestId = {}, status = {}", savedRequest.getId(), savedRequest.getStatus());

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
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден."));
    }

    // Проверка существования события
    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено."));
    }

    // Валидация события для запроса
    private void validateEventForRequest(Event event, Long userId) {
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии.");
        }

        if (event.getState() != State.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии.");
        }

        if (event.getParticipantLimit() > 0) {
            Long confirmedRequests = requestRepository
                    .countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);

            if (confirmedRequests >= event.getParticipantLimit()) {
                throw new ConflictException("У события достигнут лимит запросов на участие.");
            }
        }
    }

    // Проверка существования запроса и проверка запроса пользователя
    private Request findRequestAndCheckOwner(Long requestId, Long userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id = " + requestId + " не найден."));

        if (!Objects.equals(request.getRequester().getId(), userId)) {
            throw new NotFoundException("Запрос с id = " + requestId + " не найден у текущего пользователя.");
        }
        return request;
    }
}
