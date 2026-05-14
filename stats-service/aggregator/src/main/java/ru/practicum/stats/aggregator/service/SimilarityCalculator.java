package ru.practicum.stats.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.stats.aggregator.model.EventSimilarity;
import ru.practicum.stats.aggregator.repository.InMemorySimilarityRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityCalculator {

    private final InMemorySimilarityRepository repository;

    // Рассчитывает сходство между мероприятиями A и B по формуле:
    // similarity(A, B) = S_min(A, B) / (sqrt(S_A) * sqrt(S_B))
    public double calculateSimilarity(long eventA, long eventB) {
        double sMin = repository.getMinWeightSum(eventA, eventB).orElse(0.0);
        double sA = repository.getEventWeightSum(eventA).orElse(0.0);
        double sB = repository.getEventWeightSum(eventB).orElse(0.0);

        log.debug("calculateSimilarity: eventA={}, eventB={}, sMin={}, sA={}, sB={}", eventA, eventB, sMin, sA, sB);

        if (sA <= 0 || sB <= 0 || sMin <= 0) {
            return 0.0;
        }
        return sMin / (Math.sqrt(sA) * Math.sqrt(sB));
    }

    // Пересчитывает сходство для мероприятия со всеми другими
    public List<EventSimilarity> recalculateAllSimilaritiesForEvent(long eventId) {
        List<EventSimilarity> similarities = new ArrayList<>();

        // Получаем все мероприятия, с которыми есть S_min > 0
        List<EventSimilarity> existingPairs = repository.getAllSimilaritiesForEvent(eventId);

        for (EventSimilarity pair : existingPairs) {
            long otherEvent = (pair.getEventA() == eventId) ? pair.getEventB() : pair.getEventA();
            double score = calculateSimilarity(eventId, otherEvent);
            similarities.add(new EventSimilarity(eventId, otherEvent, score));
        }

        return similarities;
    }
}