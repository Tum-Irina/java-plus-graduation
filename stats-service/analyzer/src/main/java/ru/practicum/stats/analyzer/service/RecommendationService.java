package ru.practicum.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    // Список рекомендуемых мероприятий для пользователя на основе истории взаимодействий пользователя и сходства мероприятий
    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        log.info("Поиск рекомендаций для пользователя {}, maxResults={}", userId, maxResults);

        // Получаем мероприятия, с которыми взаимодействовал пользователь (с весами)
        List<Object[]> userInteractions = interactionRepository.findEventIdsAndWeightsByUserId(userId);
        if (userInteractions.isEmpty()) {
            log.info("Пользователь {} не имеет взаимодействий", userId);
            return Collections.emptyList();
        }

        // Получаем ID мероприятий, с которыми пользователь взаимодействовал
        Set<Long> interactedEventIds = userInteractions.stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toSet());

        // Для каждого взаимодействия пользователя находим похожие мероприятия и собираем кандидатов с их оценками
        Map<Long, Double> candidateScores = new HashMap<>();
        Map<Long, Double> userWeights = userInteractions.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Double) row[1]
                ));

        for (Long interactedEventId : interactedEventIds) {
            List<Object[]> similarEvents = similarityRepository.findTopSimilarEvents(interactedEventId, 100);
            double userWeight = userWeights.get(interactedEventId);

            for (Object[] row : similarEvents) {
                long candidateEventId = (Long) row[0];
                double similarityScore = (Double) row[1];

                // Исключаем мероприятия, с которыми пользователь уже взаимодействовал
                if (interactedEventIds.contains(candidateEventId)) {
                    continue;
                }

                // Предсказанная оценка = вес пользователя * коэффициент сходства
                double predictedScore = userWeight * similarityScore;
                candidateScores.merge(candidateEventId, predictedScore, Double::sum);
            }
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

        // Получаем похожие мероприятия из базы
        List<Object[]> similarEvents = similarityRepository.findTopSimilarEvents(eventId, maxResults * 2);

        // Фильтруем уже просмотренные и возвращаем результат
        return similarEvents.stream()
                .filter(row -> !interactedEventIds.contains((Long) row[0]))
                .limit(maxResults)
                .map(row -> RecommendedEventProto.newBuilder()
                        .setEventId((Long) row[0])
                        .setScore((Double) row[1])
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
        List<Object[]> weights = interactionRepository.findWeightsByEventIds(eventIdSet);

        // Группируем по eventId и суммируем веса
        Map<Long, Double> totalWeights = weights.stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.summingDouble(row -> (Double) row[1])
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