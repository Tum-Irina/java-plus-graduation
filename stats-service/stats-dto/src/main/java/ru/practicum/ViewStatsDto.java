package ru.practicum;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewStatsDto {

    @NotBlank(message = "Название приложения не может быть пустым")
    private String app;

    @NotBlank(message = "URI не может быть пустым")
    private String uri;

    @NotNull(message = "Количество просмотров не может быть пустым")
    @Min(value = 0, message = "Количество просмотров не может быть отрицательным")
    private Long hits;
}