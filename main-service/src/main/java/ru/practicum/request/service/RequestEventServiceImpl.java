package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequestEventServiceImpl implements RequestEventService {

    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        log.info("Получение запросов на участие в событии eventId = {} пользователем userId = {}", eventId, userId);

        // Проверка существования пользователя
        User user = findUserById(userId);

        // Проверка существования события и что пользователь является инициатором
        Event event = findEventById(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id = " + eventId + " не найдено у текущего пользователя.");
        }

        List<ParticipationRequestDto> requests = requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());

        log.info("Найдено {} запросов для события eventId = {}", requests.size(), eventId);
        return requests;
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        log.info("Изменение статуса запросов для события eventId = {} пользователем userId = {}", eventId, userId);

        // Проверка существования пользователя
        User user = findUserById(userId);

        // Проверка существования события и что пользователь является инициатором
        Event event = findEventById(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id = " + eventId + " не найдено у текущего пользователя.");
        }

        // Проверка лимита участников
        if (event.getParticipantLimit() > 0) {
            Long confirmedRequests = requestRepository
                    .countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            if (confirmedRequests >= event.getParticipantLimit()) {
                throw new ConflictException("Достигнут лимит одобренных заявок");
            }
        }

        List<Request> requestsToUpdate = requestRepository.findAllByIdIn(updateRequest.getRequestIds());

        // Проверка, что все запросы относятся к данному событию
        for (Request request : requestsToUpdate) {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new NotFoundException("Запрос с id = " + request.getId() + " не относится к событию " + eventId);
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        String status = updateRequest.getStatus();
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        for (Request request : requestsToUpdate) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус можно изменить только у заявок, находящихся в состоянии ожидания");
            }

            if ("CONFIRMED".equals(status)) {
                if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(RequestMapper.toDto(request));
                } else if (event.getParticipantLimit() > 0 && confirmedRequests < event.getParticipantLimit()) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(RequestMapper.toDto(request));
                    confirmedRequests++;
                } else {
                    request.setStatus(RequestStatus.REJECTED);
                    rejected.add(RequestMapper.toDto(request));
                }
            } else if ("REJECTED".equals(status)) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toDto(request));
            }

            requestRepository.save(request);
        }

        // Если при подтверждении лимит исчерпан, отклоняем все оставшиеся
        if ("CONFIRMED".equals(status) && event.getParticipantLimit() > 0 &&
                confirmedRequests >= event.getParticipantLimit()) {
            List<Request> pendingRequests = requestRepository.findAllByEventId(eventId).stream()
                    .filter(r -> r.getStatus() == RequestStatus.PENDING)
                    .collect(Collectors.toList());

            for (Request request : pendingRequests) {
                request.setStatus(RequestStatus.REJECTED);
                requestRepository.save(request);
                rejected.add(RequestMapper.toDto(request));
            }
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmed);
        result.setRejectedRequests(rejected);

        return result;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден."));
    }

    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено."));
    }
}