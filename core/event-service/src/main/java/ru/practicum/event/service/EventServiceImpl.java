package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.core.client.CommentClient;
import ru.practicum.core.client.RequestClient;
import ru.practicum.core.client.UserClient;
import ru.practicum.event.*;
import ru.practicum.event.dto.*;
import ru.practicum.event.params.AdminEventsParam;
import ru.practicum.event.params.PublicEventsParam;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.core.exception.ConditionsNotMetException;
import ru.practicum.core.exception.NotFoundException;
import ru.practicum.core.exception.ValidationException;
import ru.practicum.core.dto.user.UserDto;
import ru.practicum.core.dto.user.UserShortDto;
import ru.practicum.stats.client.CollectorClient;
import ru.practicum.stats.client.RecommendationsClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final CollectorClient collectorClient;
    private final RecommendationsClient recommendationsClient;
    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final CategoryRepository categoryRepository;
    private final RequestClient requestClient;
    private final CommentClient commentClient;

    @Value("${spring.application.name}")
    private String appName;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    // Получения количества комментариев
    private Long getCommentCount(Long eventId) {
        return commentClient.getCommentCountByEventId(eventId);
    }

    @Override
    public List<EventShortDto> findByInitiatorId(Long initiatorId, int from, int size) {
        log.info("Получение событий, добавленных пользователем с id: {}", initiatorId);

        if (initiatorId == null || !userClient.userExists(initiatorId)) {
            throw new NotFoundException("Инициатор события не найден");
        }
        Pageable pageable = PageRequest.of(from, size, Sort.by("eventDate"));
        List<Event> events = eventRepository.findByInitiatorId(initiatorId, pageable).getContent();

        UserShortDto initiatorShort = getUserShortDto(initiatorId);

        Map<Long, Double> ratings = getRatings(events);
        List<EventShortDto> eventsShortDto = events
                .stream()
                .map(event -> EventMapper.toShortDto(event, initiatorShort))
                .collect(Collectors.toList());
        for (EventShortDto event : eventsShortDto) {
            if (ratings.get(event.getId()) != null) {
                event.setRating(ratings.get(event.getId()));
            } else {
                event.setRating(0.0);
            }
        }
        Set<Long> eventIds = events
                .stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(eventIds);
        for (EventShortDto event : eventsShortDto) {
            if (confirmedRequests.get(event.getId()) != null) {
                event.setConfirmedRequests(confirmedRequests.get(event.getId()));
            } else {
                event.setConfirmedRequests(0L);
            }
        }
        return eventsShortDto;
    }

    @Override
    @Transactional
    public EventFullDto createEvent(NewEventDto newEventDto, Long initiatorId) {
        log.info("Создание нового события");

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория события не найдена"));

        LocalDateTime eventDate = newEventDto.getEventDate() != null ? LocalDateTime.from(FORMATTER.parse(newEventDto.getEventDate())) : null;
        if (eventDate != null && Duration.between(LocalDateTime.now(), eventDate).toHours() < 2) {
            throw new ValidationException("Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента");
        }
        if (newEventDto.getParticipantLimit() < 0) {
            throw new ValidationException("Лимит участников не может быть отрицательным");
        }
        Event event = EventMapper.toEntity(newEventDto);
        event.setInitiatorId(initiatorId);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(State.PENDING);

        Event savedEvent = eventRepository.save(event);

        UserShortDto initiatorShort = getUserShortDto(initiatorId);

        EventFullDto eventFullDto = EventMapper.toFullDto(savedEvent, initiatorShort);
        eventFullDto.setRating(0.0);
        eventFullDto.setConfirmedRequests(0L);
        log.info("Создано событие с ID: {}", savedEvent.getId());

        return eventFullDto;
    }

    @Override
    public EventFullDto findByIdAndInitiatorId(Long initiatorId, Long eventId) {
        log.info("Получение полной информации о событии с id: {}, добавленного пользователем с id {}", eventId, initiatorId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (initiatorId == null || !userClient.userExists(initiatorId)) {
            throw new NotFoundException("Инициатор события не найден");
        }

        UserShortDto initiatorShort = getUserShortDto(event.getInitiatorId());

        EventFullDto eventFullDto = EventMapper.toFullDto(event, initiatorShort);
        double rating = recommendationsClient.getEventRating(eventId);
        eventFullDto.setRating(rating);

        eventFullDto.setConfirmedRequests(getConfirmedRequestsCount(eventId));

        // Добавляем количество комментариев
        eventFullDto.setCommentCount(getCommentCount(eventId));

        return eventFullDto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventUser(UpdateEventUserRequest updateEventUserRequest, Long initiatorId, Long eventId) {
        log.info("Обновление события пользователем");
        Event oldEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (initiatorId == null || !userClient.userExists(initiatorId)) {
            throw new NotFoundException("Инициатор события не найден");
        }
        if (updateEventUserRequest.getCategory() != null && !categoryRepository.existsById(updateEventUserRequest.getCategory())) {
            throw new NotFoundException("Категория события не найдена");
        }
        LocalDateTime eventDate = updateEventUserRequest.getEventDate() != null ? LocalDateTime.from(FORMATTER.parse(updateEventUserRequest.getEventDate())) : null;
        if (eventDate != null && Duration.between(LocalDateTime.now(), eventDate).toHours() < 2) {
            throw new ValidationException("Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента");
        }
        if (oldEvent.getState() != State.CANCELED && oldEvent.getState() != State.PENDING) {
            throw new ConditionsNotMetException("Изменять можно только отмененные события или события в состоянии ожидания модерации");
        }
        if (updateEventUserRequest.getStateAction() == UserStateAction.SEND_TO_REVIEW) {
            oldEvent.setState(State.PENDING);
        } else if (updateEventUserRequest.getStateAction() == UserStateAction.CANCEL_REVIEW) {
            oldEvent.setState(State.CANCELED);
        }
        if (updateEventUserRequest.getStateAction() != UserStateAction.SEND_TO_REVIEW && updateEventUserRequest.getStateAction() != UserStateAction.CANCEL_REVIEW) {
            if (updateEventUserRequest.getAnnotation() != null) {
                oldEvent.setAnnotation(updateEventUserRequest.getAnnotation());
            }
            if (updateEventUserRequest.getCategory() != null) {
                oldEvent.setCategory(categoryRepository.getById(updateEventUserRequest.getCategory()));
            }
            if (updateEventUserRequest.getDescription() != null) {
                oldEvent.setDescription(updateEventUserRequest.getDescription());
            }
            if (updateEventUserRequest.getEventDate() != null) {
                oldEvent.setEventDate(LocalDateTime.from(FORMATTER.parse(updateEventUserRequest.getEventDate())));
            }
            if (updateEventUserRequest.getLocation() != null) {
                oldEvent.setLocation(updateEventUserRequest.getLocation());
            }
            if (updateEventUserRequest.getPaid() != null) {
                oldEvent.setPaid(updateEventUserRequest.getPaid());
            }
            if (updateEventUserRequest.getParticipantLimit() != null) {
                oldEvent.setParticipantLimit(updateEventUserRequest.getParticipantLimit());
            }
            if (updateEventUserRequest.getRequestModeration() != null) {
                oldEvent.setRequestModeration(updateEventUserRequest.getRequestModeration());
            }
            if (updateEventUserRequest.getTitle() != null) {
                oldEvent.setTitle(updateEventUserRequest.getTitle());
            }
        }

        Event updatedEvent = eventRepository.save(oldEvent);

        UserShortDto initiatorShort = getUserShortDto(updatedEvent.getInitiatorId());

        EventFullDto eventFullDto = EventMapper.toFullDto(updatedEvent, initiatorShort);
        double rating = recommendationsClient.getEventRating(eventId);
        eventFullDto.setRating(rating);

        eventFullDto.setConfirmedRequests(getConfirmedRequestsCount(eventId));

        log.info("Обновление событие с ID: {} пользователем", eventId);

        return eventFullDto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(UpdateEventAdminRequest updateEventAdminRequest, Long eventId) {
        log.info("Обновление события администратором");

        Event oldEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (updateEventAdminRequest.getCategory() != null && !categoryRepository.existsById(updateEventAdminRequest.getCategory())) {
            throw new NotFoundException("Категория события не найдена");
        }
        LocalDateTime eventDate = updateEventAdminRequest.getEventDate() != null ? LocalDateTime.from(FORMATTER.parse(updateEventAdminRequest.getEventDate())) : null;
        if (eventDate != null && Duration.between(LocalDateTime.now(), eventDate).toHours() < 2) {
            throw new ValidationException("Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента");
        }
        if (oldEvent.getState() != State.CANCELED && oldEvent.getState() != State.PENDING) {
            throw new ConditionsNotMetException("Изменять можно только отмененные события или события в состоянии ожидания модерации");
        }
        if (updateEventAdminRequest.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            if (oldEvent.getState() == State.CANCELED) {
                throw new ConditionsNotMetException("Нельзя публиковать отмененные события");
            }
            oldEvent.setState(State.PUBLISHED);
            oldEvent.setPublishedOn(LocalDateTime.now());
        } else if (updateEventAdminRequest.getStateAction() == AdminStateAction.REJECT_EVENT) {
            oldEvent.setState(State.CANCELED);
        }

        if (updateEventAdminRequest.getStateAction() != AdminStateAction.REJECT_EVENT) {
            if (updateEventAdminRequest.getAnnotation() != null) {
                oldEvent.setAnnotation(updateEventAdminRequest.getAnnotation());
            }
            if (updateEventAdminRequest.getCategory() != null) {
                oldEvent.setCategory(categoryRepository.getById(updateEventAdminRequest.getCategory()));
            }
            if (updateEventAdminRequest.getDescription() != null) {
                oldEvent.setDescription(updateEventAdminRequest.getDescription());
            }
            if (updateEventAdminRequest.getEventDate() != null) {
                oldEvent.setEventDate(LocalDateTime.from(FORMATTER.parse(updateEventAdminRequest.getEventDate())));
            }
            if (updateEventAdminRequest.getLocation() != null) {
                oldEvent.setLocation(updateEventAdminRequest.getLocation());
            }
            if (updateEventAdminRequest.getPaid() != null) {
                oldEvent.setPaid(updateEventAdminRequest.getPaid());
            }
            if (updateEventAdminRequest.getParticipantLimit() != null && updateEventAdminRequest.getStateAction() != AdminStateAction.REJECT_EVENT) {
                oldEvent.setParticipantLimit(updateEventAdminRequest.getParticipantLimit());
            }
            if (updateEventAdminRequest.getRequestModeration() != null) {
                oldEvent.setRequestModeration(updateEventAdminRequest.getRequestModeration());
            }
            if (updateEventAdminRequest.getTitle() != null) {
                oldEvent.setTitle(updateEventAdminRequest.getTitle());
            }
        }

        Event updatedEvent = eventRepository.save(oldEvent);

        UserShortDto initiatorShort = getUserShortDto(updatedEvent.getInitiatorId());

        EventFullDto eventFullDto = EventMapper.toFullDto(updatedEvent, initiatorShort);
        double rating = recommendationsClient.getEventRating(eventId);
        eventFullDto.setRating(rating);

        eventFullDto.setConfirmedRequests(getConfirmedRequestsCount(eventId));

        log.info("Обновление событие с ID: {}  администратором", eventId);

        return eventFullDto;
    }

    @Override
    public List<EventShortDto> getEventsPublic(PublicEventsParam publicEventsParam, String ip, String uri) {
        log.info("Получение опубликованных событий");
        Pageable pageable = PageRequest.of(publicEventsParam.getFrom(), publicEventsParam.getSize(), Sort.by("eventDate"));
        List<Event> events = eventRepository.getEventsPublic(publicEventsParam, pageable).getContent();
        if (events.isEmpty()) {
            throw new ValidationException("Запрос составлен некорректно");
        }

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        Map<Long, UserShortDto> usersMap = new HashMap<>();
        for (Long id : initiatorIds) {
            usersMap.put(id, getUserShortDto(id));
        }

        Map<Long, Double> ratings = getRatings(events);
        List<EventShortDto> eventsShortDto = events
                .stream()
                .map(event -> EventMapper.toShortDto(event, usersMap.get(event.getInitiatorId())))
                .collect(Collectors.toList());
        for (EventShortDto event : eventsShortDto) {
            if (ratings.get(event.getId()) != null) {
                event.setRating(ratings.get(event.getId()));
            } else {
                event.setRating(0.0);
            }
        }
        if (publicEventsParam.getSort() == SortEvents.RATING) {
            return eventsShortDto.stream()
                    .sorted(Comparator.comparing(EventShortDto::getRating).reversed())
                    .collect(Collectors.toList());
        }
        Set<Long> eventIds = events
                .stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(eventIds);
        for (EventShortDto event : eventsShortDto) {
            if (confirmedRequests.get(event.getId()) != null) {
                event.setConfirmedRequests(confirmedRequests.get(event.getId()));
            } else {
                event.setConfirmedRequests(0L);
            }
        }

        return eventsShortDto;
    }

    @Override
    public List<EventFullDto> searchEventsByAdmin(AdminEventsParam adminEventsParam) {
        log.info("Получение событий администратором");
        Pageable pageable = PageRequest.of(adminEventsParam.getFrom(), adminEventsParam.getSize(), Sort.by("eventDate"));
        List<Event> events = eventRepository.searchEventsByAdmin(adminEventsParam, pageable).getContent();

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        Map<Long, UserShortDto> usersMap = new HashMap<>();
        for (Long id : initiatorIds) {
            usersMap.put(id, getUserShortDto(id));
        }

        Map<Long, Double> ratings = getRatings(events);
        List<EventFullDto> eventsFullDto = events
                .stream()
                .map(event -> EventMapper.toFullDto(event, usersMap.get(event.getInitiatorId())))
                .collect(Collectors.toList());
        for (EventFullDto event : eventsFullDto) {
            if (ratings.get(event.getId()) != null) {
                event.setRating(ratings.get(event.getId()));
            } else {
                event.setRating(0.0);
            }
        }
        Set<Long> eventIds = events
                .stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(eventIds);
        for (EventFullDto event : eventsFullDto) {
            if (confirmedRequests.get(event.getId()) != null) {
                event.setConfirmedRequests(confirmedRequests.get(event.getId()));
            } else {
                event.setConfirmedRequests(0L);
            }
            // Добавляем количество комментариев
            event.setCommentCount(getCommentCount(event.getId()));
        }
        return eventsFullDto;
    }

    @Override
    public EventFullDto findById(Long eventId, String ip, String uri, Long userId) {
        Event event = eventRepository.findByIdPublished(eventId);
        if (event == null) {
            throw new NotFoundException("Событие не найдено или недоступно");
        }

        UserShortDto initiatorShort = getUserShortDto(event.getInitiatorId());

        EventFullDto eventFullDto = EventMapper.toFullDto(event, initiatorShort);
        double rating = recommendationsClient.getEventRating(eventId);
        eventFullDto.setRating(rating);

        eventFullDto.setConfirmedRequests(getConfirmedRequestsCount(eventId));

        // Добавляем количество комментариев
        eventFullDto.setCommentCount(getCommentCount(eventId));

        collectorClient.sendViewEvent(userId, eventId, Instant.now());
        return eventFullDto;
    }

    private Map<Long, Double> getRatings(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return new HashMap<>();
        }
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());
        return recommendationsClient.getEventsRatings(eventIds);
    }

    private Map<Long, Long> getConfirmedRequestsForEvents(Set<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return requestClient.getConfirmedRequestsCounts(eventIds);
    }

    private Long getConfirmedRequestsCount(Long eventId) {
        Set<Long> eventIds = new HashSet<>();
        eventIds.add(eventId);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(eventIds);
        return confirmedRequests.getOrDefault(eventId, 0L);
    }

    private UserShortDto getUserShortDto(Long userId) {
        UserDto userDto = userClient.getUserById(userId);
        return UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build();
    }

    @Override
    @Transactional
    public void likeEvent(Long userId, Long eventId) {
        log.info("Пользователь {} ставит лайк мероприятию {}", userId, eventId);

        boolean hasParticipated = requestClient.hasConfirmedRequest(userId, eventId);
        if (!hasParticipated) {
            throw new ConditionsNotMetException("Лайк можно поставить только на посещенное мероприятие");
        }

        collectorClient.sendLikeEvent(userId, eventId, Instant.now());
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, int maxResults) {
        log.info("Получение рекомендаций для пользователя {}, maxResults={}", userId, maxResults);

        List<ru.practicum.stats.proto.RecommendedEventProto> recommendations =
                recommendationsClient.getRecommendationsForUser(userId, maxResults);

        if (recommendations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = recommendations.stream()
                .map(ru.practicum.stats.proto.RecommendedEventProto::getEventId)
                .collect(Collectors.toList());

        List<Event> events = eventRepository.findAllByIdIn(new HashSet<>(eventIds));
        Map<Long, Event> eventMap = events.stream()
                .collect(Collectors.toMap(Event::getId, e -> e));

        Map<Long, Double> scores = recommendations.stream()
                .collect(Collectors.toMap(
                        ru.practicum.stats.proto.RecommendedEventProto::getEventId,
                        ru.practicum.stats.proto.RecommendedEventProto::getScore
                ));

        UserShortDto userShortDto = getUserShortDto(userId);

        return eventIds.stream()
                .filter(eventMap::containsKey)
                .map(id -> {
                    EventShortDto dto = EventMapper.toShortDto(eventMap.get(id), userShortDto);
                    dto.setRating(scores.getOrDefault(id, 0.0));
                    return dto;
                })
                .collect(Collectors.toList());
    }
}