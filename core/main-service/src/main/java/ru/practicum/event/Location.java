package ru.practicum.event;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
public class Location implements Serializable {

    @Min(value = -90, message = "Широта должна быть от -90 до 90 градусов")
    @Max(value = 90, message = "Широта должна быть от -90 до 90 градусов")
    private Double lat;

    @Min(value = -180, message = "Долгота должна быть от -180 до 180 градусов")
    @Max(value = 180, message = "Долгота должна быть от -180 до 180 градусов")
    private Double lon;
}
