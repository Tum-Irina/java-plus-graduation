package ru.practicum.event;

import ru.practicum.category.CategoryMapper;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.user.UserMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class EventMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    public static Event toEntity(NewEventDto newEventDto) {
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .eventDate(LocalDateTime.from(FORMATTER.parse(newEventDto.getEventDate())))
                .location(newEventDto.getLocation())
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .requestModeration(newEventDto.getRequestModeration())
                .title(newEventDto.getTitle())
                .build();
    }

    public static Event toEntity(UpdateEventUserRequest updateEventUserRequest) {
        return Event.builder()
                .annotation(updateEventUserRequest.getAnnotation())
                .description(updateEventUserRequest.getDescription())
                .eventDate(LocalDateTime.from(FORMATTER.parse(updateEventUserRequest.getEventDate())))
                .location(updateEventUserRequest.getLocation())
                .paid(updateEventUserRequest.getPaid())
                .participantLimit(updateEventUserRequest.getParticipantLimit())
                .requestModeration(updateEventUserRequest.getRequestModeration())
                .title(updateEventUserRequest.getTitle())
                .build();
    }

    public static EventShortDto toShortDto(Event event) {
        return EventShortDto.builder()
                .annotation(event.getAnnotation())
                .category(event.getCategory() != null ? CategoryMapper.toDto(event.getCategory()) : null)
                .eventDate(event.getEventDate() != null ? FORMATTER.format(event.getEventDate()) : "")
                .id(event.getId())
                .initiator(event.getInitiator() != null ? UserMapper.toShortDto(event.getInitiator()) : null)
                .paid(event.getPaid())
                .title(event.getTitle())
                .build();
    }

    public static EventFullDto toFullDto(Event event) {
        return EventFullDto.builder()
                .annotation(event.getAnnotation())
                .category(event.getCategory() != null ? CategoryMapper.toDto(event.getCategory()) : null)
                .createdOn(FORMATTER.format(event.getCreatedOn()))
                .description(event.getDescription())
                .eventDate(event.getEventDate() != null ? FORMATTER.format(event.getEventDate()) : "")
                .id(event.getId())
                .initiator(event.getInitiator() != null ? UserMapper.toShortDto(event.getInitiator()) : null)
                .location(event.getLocation())
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn() != null ? FORMATTER.format(event.getPublishedOn()) : "")
                .requestModeration(event.getRequestModeration())
                .state(event.getState().toString())
                .title(event.getTitle())
                .commentCount(0L)
                .build();
    }
}
