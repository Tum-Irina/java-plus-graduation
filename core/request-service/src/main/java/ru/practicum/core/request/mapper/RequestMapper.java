package ru.practicum.core.request.mapper;

import ru.practicum.core.dto.request.ParticipationRequestDto;
import ru.practicum.core.request.model.Request;

import java.time.format.DateTimeFormatter;

public class RequestMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ParticipationRequestDto toDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .created(request.getCreated().format(FORMATTER))
                .status(request.getStatus().toString())
                .build();
    }
}
