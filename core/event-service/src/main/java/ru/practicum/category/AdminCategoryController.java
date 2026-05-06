package ru.practicum.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/admin/categories")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        log.info("POST /admin/categories - создание категории: {}", newCategoryDto);
        return categoryService.createCategory(newCategoryDto);
    }

    @PatchMapping("/{catId}")
    public CategoryDto updateCategory(
            @PathVariable Long catId,
            @Valid @RequestBody CategoryDto categoryDto) {
        log.info("PATCH /admin/categories/{} - обновление категории: {}", catId, categoryDto);
        return categoryService.updateCategory(catId, categoryDto);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long catId) {
        log.info("DELETE /admin/categories/{} - удаление категории", catId);
        categoryService.deleteCategory(catId);
    }
}