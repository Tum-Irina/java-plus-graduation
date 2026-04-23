package ru.practicum.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {
    // ID запроса
    private Long id;

    // ID события
    private Long event;

    // ID пользователя
    private Long requester;

    // дата и время создания запроса
    private String created;

    // статус запроса
    private String status;
}
