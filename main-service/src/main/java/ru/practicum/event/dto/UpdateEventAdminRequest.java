package ru.practicum.event.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.practicum.event.Location;
import ru.practicum.event.AdminStateAction;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventAdminRequest {

    @Size(min = 20, max = 2000, message = "Аннотация должна содержать от 2 до 2000 символов")
    private String annotation;

    private Long category;

    @Size(min = 20, max = 7000, message = "Описание события должно содержать от 20 до 7000 символов")
    private String description;

    private String eventDate;

    private Location location;

    private Boolean paid;

    @PositiveOrZero(message = "Число участников не должно быть отрицательным")
    private Integer participantLimit;

    private Boolean requestModeration;

    public AdminStateAction stateAction;

    @Size(min = 3, max = 120, message = "Заголовок события должен содержать от 3 до 120 символов")
    private String title;
}
