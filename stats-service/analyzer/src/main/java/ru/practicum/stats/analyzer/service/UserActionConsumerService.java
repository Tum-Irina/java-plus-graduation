package ru.practicum.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.analyzer.entity.Interaction;
import ru.practicum.stats.analyzer.repository.InteractionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.common.ActionWeights;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionConsumerService {

    private final InteractionRepository interactionRepository;

    @KafkaListener(topics = "${stats.kafka.topics.user-actions}",
            containerFactory = "userActionKafkaListenerContainerFactory")
    @Transactional
    public void consumeUserAction(ConsumerRecord<Long, UserActionAvro> record, Acknowledgment ack) {
        try {
            Long userId = record.key();
            UserActionAvro action = record.value();
            long eventId = action.getEventId();
            double newWeight = ActionWeights.getWeight(action.getActionType());
            long timestamp = action.getTimestamp().toEpochMilli();

            log.debug("Получено UserAction: userId={}, eventId={}, weight={}", userId, eventId, newWeight);

            Optional<Interaction> existingInteraction = interactionRepository.findByUserIdAndEventId(userId, eventId);

            if (existingInteraction.isPresent()) {
                Interaction interaction = existingInteraction.get();
                if (interaction.getWeight() >= newWeight || interaction.getUpdatedAt() >= timestamp) {
                    log.debug("Пропускаем устаревшее или меньшее по весу событие");
                    ack.acknowledge();
                    return;
                }
                interaction.setWeight(newWeight);
                interaction.setUpdatedAt(timestamp);
                interactionRepository.save(interaction);
            } else {
                Interaction interaction = Interaction.builder()
                        .userId(userId)
                        .eventId(eventId)
                        .weight(newWeight)
                        .updatedAt(timestamp)
                        .build();
                interactionRepository.save(interaction);
            }

            ack.acknowledge();
            log.debug("UserAction сохранен: userId={}, eventId={}, weight={}", userId, eventId, newWeight);

        } catch (Exception e) {
            log.error("Ошибка обработки UserAction", e);
            ack.acknowledge();
        }
    }
}