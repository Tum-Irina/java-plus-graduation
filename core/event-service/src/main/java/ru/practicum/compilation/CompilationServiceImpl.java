package ru.practicum.compilation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.client.RequestClient;
import ru.practicum.event.Event;
import ru.practicum.event.EventMapper;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.core.exception.NotFoundException;
import ru.practicum.stats.client.RecommendationsClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final RequestClient requestClient;
    private final RecommendationsClient recommendationsClient;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        log.info("Создание подборки: {}", newCompilationDto.getTitle());

        Compilation compilation = CompilationMapper.toEntity(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            Set<Event> events = findEventsByIds(newCompilationDto.getEvents());
            compilation.setEvents(events);
        }

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Подборка создана с ID: {}", savedCompilation.getId());

        Compilation compilationWithEvents = getCompilationWithEvents(savedCompilation.getId());

        return convertCompilationToDto(compilationWithEvents);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest request) {
        log.info("Обновление подборки ID: {}", compilationId);

        Compilation compilation = getCompilationWithEvents(compilationId);

        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }

        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }

        if (request.getEvents() != null) {
            Set<Event> events = findEventsByIds(request.getEvents());
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Подборка ID {} обновлена", compilationId);

        return convertCompilationToDto(updatedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compilationId) {
        log.info("Удаление подборки ID: {}", compilationId);

        checkCompilationExists(compilationId);
        compilationRepository.deleteById(compilationId);
        log.info("Подборка ID {} удалена", compilationId);
    }

    @Override
    public List<CompilationDto> getAllCompilations(Boolean pinned, Pageable pageable) {
        log.info("Получение подборок, pinned: {}", pinned);

        List<Compilation> compilations = compilationRepository
                .findAllCompilationsWithEvents(pinned, pageable);

        log.info("Найдено {} подборок", compilations.size());

        if (compilations.isEmpty()) {
            return Collections.emptyList();
        }

        return convertCompilationsToDtoList(compilations);
    }

    @Override
    public CompilationDto getCompilationById(Long compilationId) {
        log.info("Получение подборки ID: {}", compilationId);

        Compilation compilation = getCompilationWithEvents(compilationId);
        return convertCompilationToDto(compilation);
    }

    private CompilationDto convertCompilationToDto(Compilation compilation) {
        Set<EventShortDto> events = new HashSet<>();

        if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
            events = convertEventsToShortDtos(new ArrayList<>(compilation.getEvents()));
        }

        return CompilationMapper.toDto(compilation, events);
    }

    private List<CompilationDto> convertCompilationsToDtoList(List<Compilation> compilations) {
        List<Event> allEvents = compilations.stream()
                .filter(c -> c.getEvents() != null && !c.getEvents().isEmpty())
                .flatMap(c -> c.getEvents().stream())
                .distinct()
                .toList();

        Map<Long, Double> ratingsMap = getRatingsForEvents(
                allEvents.stream().map(Event::getId).collect(Collectors.toSet())
        );
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsForEvents(
                allEvents.stream().map(Event::getId).collect(Collectors.toSet())
        );

        Map<Long, EventShortDto> eventDtoMap = allEvents.stream()
                .collect(Collectors.toMap(
                        Event::getId,
                        event -> createEventShortDtoWithStats(event, ratingsMap, confirmedRequestsMap)
                ));

        return compilations.stream()
                .map(compilation -> {
                    Set<EventShortDto> events = new HashSet<>();

                    if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
                        events = compilation.getEvents().stream()
                                .map(event -> eventDtoMap.get(event.getId()))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                    }

                    // Вызываем маппер
                    return CompilationMapper.toDto(compilation, events);
                })
                .collect(Collectors.toList());
    }

    private Set<EventShortDto> convertEventsToShortDtos(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toSet());

        Map<Long, Double> ratingsMap = getRatingsForEvents(eventIds);
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsForEvents(eventIds);

        return events.stream()
                .map(event -> createEventShortDtoWithStats(event, ratingsMap, confirmedRequestsMap))
                .collect(Collectors.toSet());
    }

    private EventShortDto createEventShortDtoWithStats(Event event,
                                                       Map<Long, Double> ratingsMap,
                                                       Map<Long, Long> confirmedRequestsMap) {
        EventShortDto dto = EventMapper.toShortDto(event, null);
        if (dto != null) {
            dto.setRating(ratingsMap.getOrDefault(event.getId(), 0.0));  // !!! views → rating !!!
            dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
        }
        return dto;
    }

    private Map<Long, Double> getRatingsForEvents(Set<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<Long> ids = new ArrayList<>(eventIds);
            return recommendationsClient.getEventsRatings(ids);
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинги мероприятий: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<Long, Long> getConfirmedRequestsForEvents(Set<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return requestClient.getConfirmedRequestsCounts(eventIds);
    }

    private Compilation getCompilationWithEvents(Long compilationId) {
        return compilationRepository.findByIdWithEvents(compilationId)
                .orElseThrow(() -> {
                    log.error("Подборка с ID {} не найдена", compilationId);
                    return new NotFoundException(
                            String.format("Подборка с id=%d не найдена", compilationId)
                    );
                });
    }

    private void checkCompilationExists(Long compilationId) {
        if (!compilationRepository.existsById(compilationId)) {
            log.error("Подборка с ID {} не найдена", compilationId);
            throw new NotFoundException(
                    String.format("Подборка с id=%d не найдена", compilationId)
            );
        }
    }

    private Set<Event> findEventsByIds(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Event> events = eventRepository.findAllByIdIn(eventIds);

        if (events.size() != eventIds.size()) {
            Set<Long> foundIds = events.stream()
                    .map(Event::getId)
                    .collect(Collectors.toSet());

            Set<Long> notFoundIds = eventIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());

            log.error("События с ID {} не найдены", notFoundIds);
            throw new NotFoundException(
                    String.format("События с id=%s не найдены", notFoundIds)
            );
        }

        return new HashSet<>(events);
    }
}