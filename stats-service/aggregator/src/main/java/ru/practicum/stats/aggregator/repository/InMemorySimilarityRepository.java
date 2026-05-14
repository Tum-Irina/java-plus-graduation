package ru.practicum.stats.aggregator.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ru.practicum.stats.aggregator.model.EventSimilarity;
import ru.practicum.stats.aggregator.model.UserActionWeight;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class InMemorySimilarityRepository {

    // Хранилище частных сумм S_a (сумма весов для мероприятия)
    private final Map<Long, Double> eventWeightSums = new ConcurrentHashMap<>();

    // Хранилище сумм минимальных весов S_min(A, B) для пар мероприятий
    private final Map<Long, Map<Long, Double>> minWeightSums = new ConcurrentHashMap<>();

    // Хранилище весов пользователей для мероприятий (с timestamp)
    private final Map<Long, Map<Long, UserActionWeight>> userEventWeights = new ConcurrentHashMap<>();

    // Методы для eventWeightSums
    public Optional<Double> getEventWeightSum(long eventId) {
        return Optional.ofNullable(eventWeightSums.get(eventId));
    }

    public void updateEventWeightSum(long eventId, double delta) {
        eventWeightSums.merge(eventId, delta, Double::sum);
        log.debug("Обновлена сумма весов для события {}: +{}, итого={}", eventId, delta, eventWeightSums.get(eventId));
    }

    // Методы для minWeightSums
    public Optional<Double> getMinWeightSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        Map<Long, Double> inner = minWeightSums.get(first);
        if (inner == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(inner.get(second));
    }

    public void updateMinWeightSum(long eventA, long eventB, double delta) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minWeightSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, Double::sum);
        log.debug("Обновлена сумма минимальных весов для пары ({}, {}): +{}, итого={}",
                first, second, delta, minWeightSums.get(first).get(second));
    }

    // Методы для userEventWeights (работа с UserActionWeight)
    public Optional<UserActionWeight> getUserEventWeight(long userId, long eventId) {
        Map<Long, UserActionWeight> userEvents = userEventWeights.get(userId);
        if (userEvents == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(userEvents.get(eventId));
    }

    public void updateUserEventWeight(long userId, long eventId, double newWeight, long timestamp) {
        UserActionWeight weight = new UserActionWeight(userId, eventId, newWeight, timestamp);
        userEventWeights.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(eventId, weight);
        log.debug("Обновлен вес пользователя {} для события {}: {}, timestamp={}", userId, eventId, newWeight, timestamp);
    }

    public Map<Long, Double> getUserEvents(long userId) {
        Map<Long, UserActionWeight> userEvents = userEventWeights.get(userId);
        if (userEvents == null) {
            return Collections.emptyMap();
        }
        return userEvents.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getWeight()));
    }

    // Методы для получения всех пар
    public List<EventSimilarity> getAllSimilaritiesForEvent(long eventId) {
        List<EventSimilarity> result = new ArrayList<>();

        // Ищем где eventId = first
        Map<Long, Double> firstMap = minWeightSums.get(eventId);
        if (firstMap != null) {
            for (Map.Entry<Long, Double> entry : firstMap.entrySet()) {
                if (entry.getValue() > 0) {
                    double score = calculateScore(eventId, entry.getKey(), entry.getValue());
                    result.add(new EventSimilarity(eventId, entry.getKey(), score));
                }
            }
        }

        // Ищем где eventId = second
        for (Map.Entry<Long, Map<Long, Double>> firstEntry : minWeightSums.entrySet()) {
            long first = firstEntry.getKey();
            if (first == eventId) continue;
            Double value = firstEntry.getValue().get(eventId);
            if (value != null && value > 0) {
                double score = calculateScore(first, eventId, value);
                result.add(new EventSimilarity(first, eventId, score));
            }
        }

        return result;
    }

    private double calculateScore(long eventA, long eventB, double sMin) {
        double sA = eventWeightSums.getOrDefault(eventA, 0.0);
        double sB = eventWeightSums.getOrDefault(eventB, 0.0);
        if (sA <= 0 || sB <= 0 || sMin <= 0) {
            return 0.0;
        }
        return sMin / (Math.sqrt(sA) * Math.sqrt(sB));
    }

    public Map<Long, Map<Long, Double>> getAllMinWeightSums() {
        return minWeightSums;
    }

    public Map<Long, Double> getAllEventWeightSums() {
        return eventWeightSums;
    }
}