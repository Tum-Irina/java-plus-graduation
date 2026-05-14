package ru.practicum.stats.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.stats.proto.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationsClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub recommendationsStub;

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = recommendationsStub.getRecommendationsForUser(request);
            List<RecommendedEventProto> result = new ArrayList<>();
            iterator.forEachRemaining(result::add);

            log.debug("Получено {} рекомендаций для пользователя {}", result.size(), userId);
            return result;

        } catch (Exception e) {
            log.error("Ошибка при получении рекомендаций для пользователя {}", userId, e);
            throw new RuntimeException("Failed to get recommendations from Analyzer", e);
        }
    }

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = recommendationsStub.getSimilarEvents(request);
            List<RecommendedEventProto> result = new ArrayList<>();
            iterator.forEachRemaining(result::add);

            log.debug("Получено {} похожих мероприятий для eventId={}", result.size(), eventId);
            return result;

        } catch (Exception e) {
            log.error("Ошибка при получении похожих мероприятий для eventId={}", eventId, e);
            throw new RuntimeException("Failed to get similar events from Analyzer", e);
        }
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventIds(eventIds)
                    .build();

            Iterator<RecommendedEventProto> iterator = recommendationsStub.getInteractionsCount(request);
            List<RecommendedEventProto> result = new ArrayList<>();
            iterator.forEachRemaining(result::add);

            log.debug("Получено {} результатов для событий {}", result.size(), eventIds);
            return result;

        } catch (Exception e) {
            log.error("Ошибка при получении количества взаимодействий для eventIds={}", eventIds, e);
            throw new RuntimeException("Failed to get interactions count from Analyzer", e);
        }
    }

    public double getEventRating(long eventId) {
        List<RecommendedEventProto> result = getInteractionsCount(List.of(eventId));
        if (result.isEmpty()) {
            return 0.0;
        }
        return result.get(0).getScore();
    }

    public java.util.Map<Long, Double> getEventsRatings(List<Long> eventIds) {
        List<RecommendedEventProto> result = getInteractionsCount(eventIds);
        return result.stream()
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore
                ));
    }
}