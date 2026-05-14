package ru.practicum.stats.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import ru.practicum.stats.aggregator.model.UserActionWeight;
import ru.practicum.stats.aggregator.repository.InMemorySimilarityRepository;
import ru.practicum.stats.aggregator.kafka.KafkaEventProducer;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.common.ActionWeights;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationService {

    private final InMemorySimilarityRepository repository;
    private final SimilarityCalculator similarityCalculator;
    private final KafkaEventProducer kafkaEventProducer;

    @KafkaListener(topics = "${stats.kafka.topics.user-actions}",
            containerFactory = "kafkaListenerContainerFactory")
    public void processUserAction(ConsumerRecord<Long, UserActionAvro> record, Acknowledgment ack) {
        try {
            Long userId = record.key();
            UserActionAvro action = record.value();

            log.debug("Получено событие: userId={}, eventId={}, actionType={}",
                    userId, action.getEventId(), action.getActionType());

            long eventId = action.getEventId();
            double newWeight = ActionWeights.getWeight(action.getActionType());

            Instant actionTimestamp = action.getTimestamp();
            long timestampMillis = actionTimestamp.toEpochMilli();

            // Получаем старый вес пользователя для этого мероприятия
            Optional<UserActionWeight> oldWeightOpt = repository.getUserEventWeight(userId, eventId);

            // Проверяем, нужно ли обновлять вес
            if (oldWeightOpt.isPresent()) {
                UserActionWeight old = oldWeightOpt.get();
                if (old.getWeight() >= newWeight || old.getTimestamp() >= timestampMillis) {
                    log.debug("Пропускаем устаревшее или меньшее по весу событие");
                    ack.acknowledge();
                    return;
                }
            }

            // Обновляем вес пользователя
            repository.updateUserEventWeight(userId, eventId, newWeight, timestampMillis);

            // Обновляем S_A (сумму весов для мероприятия)
            double weightDelta = newWeight - (oldWeightOpt.isPresent() ? oldWeightOpt.get().getWeight() : 0.0);
            repository.updateEventWeightSum(eventId, weightDelta);

            // Получаем все мероприятия, с которыми взаимодействовал пользователь
            Map<Long, Double> userEvents = repository.getUserEvents(userId);
            double oldWeight = oldWeightOpt.isPresent() ? oldWeightOpt.get().getWeight() : 0.0;

            log.info("userId={}, eventId={}, oldWeight={}, newWeight={}", userId, eventId, oldWeight, newWeight);
            log.info("userEvents (все мероприятия пользователя): {}", userEvents);
            log.info("Текущий S_A для eventId={}: {}", eventId, repository.getEventWeightSum(eventId).orElse(0.0));

            // Для каждого другого мероприятия обновляем S_min и пересчитываем сходство
            for (Map.Entry<Long, Double> entry : userEvents.entrySet()) {
                long otherEventId = entry.getKey();
                double otherWeight = entry.getValue();

                if (otherEventId == eventId) continue;

                double oldMin = Math.min(oldWeight, otherWeight);
                double newMin = Math.min(newWeight, otherWeight);
                double minDelta = newMin - oldMin;

                log.info("Пара ({}, {}): otherWeight={}, oldMin={}, newMin={}, minDelta={}",
                        eventId, otherEventId, otherWeight, oldMin, newMin, minDelta);

                if (minDelta != 0) {
                    double currentSMin = repository.getMinWeightSum(eventId, otherEventId).orElse(0.0);
                    log.info("ДО обновления S_min({}, {})={}", eventId, otherEventId, currentSMin);
                    repository.updateMinWeightSum(eventId, otherEventId, minDelta);
                    double newSMin = repository.getMinWeightSum(eventId, otherEventId).orElse(0.0);
                    log.info("ПОСЛЕ обновления S_min({}, {})={}", eventId, otherEventId, newSMin);
                }

                double newScore = similarityCalculator.calculateSimilarity(eventId, otherEventId);
                log.info("Рассчитано сходство: score={}", newScore);
                kafkaEventProducer.sendEventSimilarity(eventId, otherEventId, newScore, actionTimestamp);
            }

            ack.acknowledge();
            log.debug("Событие обработано: userId={}, eventId={}, новый вес={}", userId, eventId, newWeight);

        } catch (Exception e) {
            log.error("Ошибка обработки события", e);
            ack.acknowledge();
        }
    }
}