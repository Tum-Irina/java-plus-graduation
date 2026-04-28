package ru.practicum.compilation;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompilationRequest {
    // Список ID событий
    private Set<Long> events;

    // Закрепление подборки на главной странице: true - если, подборка закреплена
    private Boolean pinned;

    // Заголовок подборки
    @Size(min = 1, max = 50, message = "Заголовок подборки должен содержать от 1 до 50 символов.")
    private String title;
}
