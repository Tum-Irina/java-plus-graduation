package ru.practicum.category;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {

    CategoryDto createCategory(NewCategoryDto newCategoryDto);

    CategoryDto updateCategory(Long categoryId, CategoryDto categoryDto);

    void deleteCategory(Long categoryId);

    List<CategoryDto> getAllCategories(Pageable pageable);

    CategoryDto getCategoryById(Long categoryId);
}