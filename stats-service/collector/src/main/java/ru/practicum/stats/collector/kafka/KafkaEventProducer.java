package ru.practicum.stats.collector.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<Long, Object> kafkaTemplate;

    @Value("${stats.kafka.topics.user-actions}")
    private String userActionsTopic;

    public void send(Long key, UserActionAvro event) {
        kafkaTemplate.send(userActionsTopic, key, event);
        log.debug("Отправлено событие в топик {}: key={}, event={}", userActionsTopic, key, event);
    }
}