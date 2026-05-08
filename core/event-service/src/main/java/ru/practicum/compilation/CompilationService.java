package ru.practicum.compilation;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CompilationService {

    CompilationDto createCompilation(NewCompilationDto newCompilationDto);

    CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest request);

    void deleteCompilation(Long compilationId);

    List<CompilationDto> getAllCompilations(Boolean pinned, Pageable pageable);

    CompilationDto getCompilationById(Long compilationId);
}