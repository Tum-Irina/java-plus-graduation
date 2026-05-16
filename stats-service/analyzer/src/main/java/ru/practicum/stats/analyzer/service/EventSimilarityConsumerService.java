package ru.practicum.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.analyzer.entity.EventSimilarityEntity;
import ru.practicum.stats.analyzer.repository.SimilarityRepository;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityConsumerService {

    private final SimilarityRepository similarityRepository;

    @KafkaListener(topics = "${stats.kafka.topics.events-similarity}",
                   containerFactory = "eventsSimilarityKafkaListenerContainerFactory")
    @Transactional
    public void consumeEventSimilarity(ConsumerRecord<String, EventsSimilarityAvro> record, Acknowledgment ack) {
        try {
            EventsSimilarityAvro similarity = record.value();
            long eventA = similarity.getEventA();
            long eventB = similarity.getEventB();
            double score = similarity.getScore();
            long timestamp = similarity.getTimestamp().toEpochMilli();

            log.debug("Получено EventsSimilarity: eventA={}, eventB={}, score={}", eventA, eventB, score);

            Optional<EventSimilarityEntity> existingEntity = similarityRepository.findByEventAAndEventB(eventA, eventB);

            if (score <= 0) {
                existingEntity.ifPresent(similarityRepository::delete);
                ack.acknowledge();
                return;
            }

            if (existingEntity.isPresent()) {
                EventSimilarityEntity entity = existingEntity.get();
                if (entity.getUpdatedAt() >= timestamp) {
                    log.debug("Пропускаем устаревшее событие сходства");
                    ack.acknowledge();
                    return;
                }
                entity.setScore(score);
                entity.setUpdatedAt(timestamp);
                similarityRepository.save(entity);
            } else {
                EventSimilarityEntity entity = EventSimilarityEntity.builder()
                        .eventA(eventA)
                        .eventB(eventB)
                        .score(score)
                        .updatedAt(timestamp)
                        .build();
                similarityRepository.save(entity);
            }

            ack.acknowledge();
            log.debug("EventsSimilarity сохранен: eventA={}, eventB={}, score={}", eventA, eventB, score);

        } catch (Exception e) {
            log.error("Ошибка обработки EventsSimilarity", e);
            throw new RuntimeException("Ошибка обработки EventsSimilarity", e);
        }
    }
}