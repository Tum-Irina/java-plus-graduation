package ru.practicum.core.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.client.EventClient;
import ru.practicum.core.client.UserClient;
import ru.practicum.core.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.core.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.core.dto.request.ParticipationRequestDto;
import ru.practicum.core.exception.ConflictException;
import ru.practicum.core.exception.NotFoundException;
import ru.practicum.core.request.mapper.RequestMapper;
import ru.practicum.core.request.model.Request;
import ru.practicum.core.request.model.RequestStatus;
import ru.practicum.core.request.repository.RequestRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequestEventServiceImpl implements RequestEventService {

    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        log.info("Получение запросов на участие в событии eventId = {} пользователем userId = {}", eventId, userId);

        // Проверка существования пользователя
        findUserById(userId);

        // Проверка существования события и что пользователь является инициатором
        findEventById(eventId);

        Long initiatorId = eventClient.getEventInitiator(eventId);
        if (!initiatorId.equals(userId)) {
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
        findUserById(userId);

        // Проверка существования события и что пользователь является инициатором
        findEventById(eventId);

        Long initiatorId = eventClient.getEventInitiator(eventId);
        if (!initiatorId.equals(userId)) {
            throw new NotFoundException("Событие с id = " + eventId + " не найдено у текущего пользователя.");
        }

        Integer participantLimit = eventClient.getEventParticipantLimit(eventId);
        Boolean requestModeration = eventClient.getEventRequestModeration(eventId);

        // Проверка лимита участников
        if (participantLimit > 0) {
            Long confirmedRequests = requestRepository
                    .countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            if (confirmedRequests >= participantLimit) {
                throw new ConflictException("Достигнут лимит одобренных заявок");
            }
        }

        List<Request> requestsToUpdate = requestRepository.findAllByIdIn(updateRequest.getRequestIds());

        // Проверка, что все запросы относятся к данному событию
        for (Request request : requestsToUpdate) {
            if (!request.getEventId().equals(eventId)) {
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
                if (participantLimit == 0 || !requestModeration) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(RequestMapper.toDto(request));
                } else if (participantLimit > 0 && confirmedRequests < participantLimit) {
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
        if ("CONFIRMED".equals(status) && participantLimit > 0 &&
                confirmedRequests >= participantLimit) {
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

    private void findUserById(Long userId) {
        if (!userClient.userExists(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.");
        }
    }

    private void findEventById(Long eventId) {
        if (!eventClient.eventExists(eventId)) {
            throw new NotFoundException("Событие с id = " + eventId + " не найдено.");
        }
    }
}