package ru.practicum.category;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    List<Category> findAllBy(Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.category.id = :categoryId")
    Long countEventsByCategoryId(@Param("categoryId") Long categoryId);

    Boolean existsByName(String name);
}