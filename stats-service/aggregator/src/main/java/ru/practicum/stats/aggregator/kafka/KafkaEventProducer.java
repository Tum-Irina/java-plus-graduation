package ru.practicum.stats.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, EventsSimilarityAvro> kafkaTemplate;

    @Value("${stats.kafka.topics.events-similarity}")
    private String eventsSimilarityTopic;

    public void sendEventSimilarity(long eventA, long eventB, double score, Instant timestamp) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        EventsSimilarityAvro event = EventsSimilarityAvro.newBuilder()
                .setEventA(first)
                .setEventB(second)
                .setScore(score)
                .setTimestamp(timestamp)
                .build();

        String key = first + "_" + second;
        kafkaTemplate.send(eventsSimilarityTopic, key, event);
        log.debug("Отправлено EventsSimilarityAvro: key={}, eventA={}, eventB={}, score={}",
                key, first, second, score);
    }
}