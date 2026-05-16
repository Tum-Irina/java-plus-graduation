package ru.practicum.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.analyzer.projection.EventWeightByEventIdProjection;
import ru.practicum.stats.analyzer.projection.EventWeightProjection;
import ru.practicum.stats.analyzer.projection.SimilarEventProjection;
import ru.practicum.stats.analyzer.projection.SourceSimilarEventProjection;
import ru.practicum.stats.analyzer.repository.InteractionRepository;
import ru.practicum.stats.analyzer.repository.SimilarityRepository;
import ru.practicum.stats.proto.RecommendedEventProto;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final InteractionRepository interactionRepository;
    private final SimilarityRepository similarityRepository;

    @Value("${recommendations.max-similar-events-per-interaction:100}")
    private int maxSimilarEventsPerInteraction;

    // Список рекомендуемых мероприятий для пользователя на основе истории взаимодействий пользователя и сходства мероприятий
    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        log.info("Поиск рекомендаций для пользователя {}, maxResults={}", userId, maxResults);

        // Получаем мероприятия, с которыми взаимодействовал пользователь (с весами)
        List<EventWeightProjection> userInteractions = interactionRepository.findEventIdsAndWeightsByUserId(userId);
        if (userInteractions.isEmpty()) {
            log.info("Пользователь {} не имеет взаимодействий", userId);
            return Collections.emptyList();
        }

        Set<Long> interactedEventIds = userInteractions.stream()
                .map(EventWeightProjection::getEventId)
                .collect(Collectors.toSet());

        List<Long> eventIdsList = new ArrayList<>(interactedEventIds);
        List<SourceSimilarEventProjection> allSimilarEvents =
                similarityRepository.findTopSimilarEventsForMultipleEvents(eventIdsList, maxSimilarEventsPerInteraction);

        Map<Long, Double> userWeights = userInteractions.stream()
                .collect(Collectors.toMap(
                        EventWeightProjection::getEventId,
                        EventWeightProjection::getWeight
                ));

        Map<Long, Double> candidateScores = new HashMap<>();

        for (SourceSimilarEventProjection similar : allSimilarEvents) {
            long sourceEventId = similar.getSourceEventId();
            long candidateEventId = similar.getOtherEventId();
            double similarityScore = similar.getScore();

            if (interactedEventIds.contains(candidateEventId)) {
                continue;
            }

            double userWeight = userWeights.getOrDefault(sourceEventId, 0.0);
            if (userWeight <= 0) {
                continue;
            }

            double predictedScore = userWeight * similarityScore;
            candidateScores.merge(candidateEventId, predictedScore, Double::sum);
        }

        // Сортируем по убыванию оценки и ограничиваем результат
        return candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    // Список мероприятий, похожих на указанное, исключая те, с которыми пользователь уже взаимодействовал
    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        log.info("Поиск похожих мероприятий для eventId={}, userId={}, maxResults={}", eventId, userId, maxResults);

        // Получаем мероприятия, с которыми пользователь уже взаимодействовал
        Set<Long> interactedEventIds = interactionRepository.findEventIdsByUserId(userId);
        List<Long> excludedIds = new ArrayList<>(interactedEventIds);

        List<SimilarEventProjection> similarEvents = similarityRepository.findTopSimilarEventsExcluding(
                eventId, excludedIds, maxResults
        );

        return similarEvents.stream()
                .map(event -> RecommendedEventProto.newBuilder()
                        .setEventId(event.getOtherEventId())
                        .setScore(event.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    // Количество взаимодействий (сумма весов) для указанных мероприятий
    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        log.info("Получение количества взаимодействий для eventIds={}", eventIds);

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> eventIdSet = new HashSet<>(eventIds);
        List<EventWeightByEventIdProjection> weights = interactionRepository.findWeightsByEventIds(eventIdSet);

        // Группируем по eventId и суммируем веса
        Map<Long, Double> totalWeights = weights.stream()
                .collect(Collectors.groupingBy(
                        EventWeightByEventIdProjection::getEventId,
                        Collectors.summingDouble(EventWeightByEventIdProjection::getWeight)
                ));

        // Возвращаем результат в порядке запрошенных ID
        return eventIds.stream()
                .map(eventId -> RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(totalWeights.getOrDefault(eventId, 0.0))
                        .build())
                .collect(Collectors.toList());
    }
}