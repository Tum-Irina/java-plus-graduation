package ru.practicum.event.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.event.Location;
import ru.practicum.event.UserStateAction;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventUserRequest {

    // Новая аннотация
    @Size(min = 20, max = 2000, message = "Аннотация должна содержать от 20 до 2000 символов")
    private String annotation;

    // Новая категория
    private Long category;

    // Новое описание
    @Size(min = 20, max = 7000, message = "Описание события должно содержать от 20 до 7000 символов")
    private String description;

    // Новые дата и время
    private String eventDate;

    // Локация проведения события
    private Location location;

    // Новое значение флага о платности мероприятия
    private Boolean paid;

    // Новый лимит пользователей
    @PositiveOrZero(message = "Число участников не должно быть отрицательным")
    private Integer participantLimit;

    // Пре-модерация заявок на участие: true - требуется модерация
    private Boolean requestModeration;

    public UserStateAction stateAction;

    // Новый заголовок
    @Size(min = 3, max = 120, message = "Заголовок события должен содержать от 3 до 120 символов")
    private String title;
}
