package ru.practicum.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Создание новой категории с именем: {}", newCategoryDto.getName());

        // Проверка уникальности имени
        checkNameUniqueness(newCategoryDto.getName());

        Category category = CategoryMapper.toEntity(newCategoryDto);
        Category savedCategory = categoryRepository.save(category);
        log.info("Категория создана с ID: {}", savedCategory.getId());

        return CategoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long categoryId, CategoryDto categoryDto) {
        log.info("Обновление категории с ID: {}, новые данные: {}", categoryId, categoryDto);

        // Получение категории с проверкой существования
        Category category = getCategoryOrThrow(categoryId);

        // Проверка уникальности имени, если оно изменилось
        if (!category.getName().equals(categoryDto.getName())) {
            checkNameUniqueness(categoryDto.getName());
        }

        category.setName(categoryDto.getName());
        Category updatedCategory = categoryRepository.save(category);
        log.info("Категория с ID {} успешно обновлена", categoryId);

        return CategoryMapper.toDto(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        log.info("Удаление категории с ID: {}", categoryId);

        // Проверка существования категории
        Category category = getCategoryOrThrow(categoryId);

        // Проверка, что категория не используется в событиях
        checkCategoryNotUsedInEvents(categoryId);

        categoryRepository.delete(category);
        log.info("Категория с ID {} успешно удалена", categoryId);
    }

    @Override
    public List<CategoryDto> getAllCategories(Pageable pageable) {
        log.info("Получение всех категорий с пагинацией: {}", pageable);

        List<Category> categories = categoryRepository.findAllBy(pageable);
        log.info("Найдено {} категорий", categories.size());

        return categories.stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(Long categoryId) {
        log.info("Получение категории по ID: {}", categoryId);

        Category category = getCategoryOrThrow(categoryId);
        return CategoryMapper.toDto(category);
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("Категория с ID {} не найдена", categoryId);
                    return new NotFoundException(String.format("Категория с ID %d не найдена", categoryId));
                });
    }

    private void checkNameUniqueness(String name) {
        if (categoryRepository.existsByName(name)) {
            log.warn("Категория с именем '{}' уже существует", name);
            throw new DataIntegrityViolationException(
                    String.format("Категория с именем '%s' уже существует", name)
            );
        }
    }

    private void checkCategoryNotUsedInEvents(Long categoryId) {
        Long eventsCount = categoryRepository.countEventsByCategoryId(categoryId);
        if (eventsCount > 0) {
            log.warn("Невозможно удалить категорию с ID {}: используется в {} событиях",
                    categoryId, eventsCount);
            throw new ConflictException(
                    String.format("Категория с ID %d используется в событиях и не может быть удалена", categoryId)
            );
        }
    }
}