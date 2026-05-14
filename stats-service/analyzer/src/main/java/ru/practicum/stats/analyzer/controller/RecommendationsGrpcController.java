package ru.practicum.stats.analyzer.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.stats.analyzer.service.RecommendationService;
import ru.practicum.stats.proto.*;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            long userId = request.getUserId();
            int maxResults = request.getMaxResults();

            log.info("gRPC getRecommendationsForUser: userId={}, maxResults={}", userId, maxResults);

            List<RecommendedEventProto> recommendations = recommendationService.getRecommendationsForUser(userId, maxResults);

            for (RecommendedEventProto recommendation : recommendations) {
                responseObserver.onNext(recommendation);
            }
            responseObserver.onCompleted();

            log.info("Отправлено {} рекомендаций для пользователя {}", recommendations.size(), userId);

        } catch (Exception e) {
            log.error("Ошибка в getRecommendationsForUser", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            long eventId = request.getEventId();
            long userId = request.getUserId();
            int maxResults = request.getMaxResults();

            log.info("gRPC getSimilarEvents: eventId={}, userId={}, maxResults={}", eventId, userId, maxResults);

            List<RecommendedEventProto> similarEvents = recommendationService.getSimilarEvents(eventId, userId, maxResults);

            for (RecommendedEventProto event : similarEvents) {
                responseObserver.onNext(event);
            }
            responseObserver.onCompleted();

            log.info("Отправлено {} похожих мероприятий для eventId={}", similarEvents.size(), eventId);

        } catch (Exception e) {
            log.error("Ошибка в getSimilarEvents", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<Long> eventIds = request.getEventIdsList();

            log.info("gRPC getInteractionsCount: eventIds={}", eventIds);

            List<RecommendedEventProto> interactions = recommendationService.getInteractionsCount(eventIds);

            for (RecommendedEventProto interaction : interactions) {
                responseObserver.onNext(interaction);
            }
            responseObserver.onCompleted();

            log.info("Отправлено {} результатов для getInteractionsCount", interactions.size());

        } catch (Exception e) {
            log.error("Ошибка в getInteractionsCount", e);
            responseObserver.onError(e);
        }
    }
}