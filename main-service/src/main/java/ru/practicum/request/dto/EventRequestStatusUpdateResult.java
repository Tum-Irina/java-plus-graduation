package ru.practicum.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateResult {

    // Список подтвержденных заявок
    private List<ParticipationRequestDto> confirmedRequests;

    // Список отклоненных заявок
    private List<ParticipationRequestDto> rejectedRequests;
}
