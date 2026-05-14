package ru.practicum.stats.client;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.stats.proto.ActionTypeProto;
import ru.practicum.stats.proto.UserActionControllerGrpc;
import ru.practicum.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub userActionStub;

    public void collectUserAction(long userId, long eventId, ActionTypeProto actionType, Instant timestamp) {
        try {
            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(timestamp.getEpochSecond())
                            .setNanos(timestamp.getNano())
                            .build())
                    .build();

            Empty response = userActionStub.collectUserAction(request);
            log.debug("Отправлено действие пользователя: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionType);

        } catch (Exception e) {
            log.error("Ошибка при отправке действия пользователя в Collector: userId={}, eventId={}",
                    userId, eventId, e);
            throw new RuntimeException("Failed to send user action to Collector", e);
        }
    }

    public void sendViewEvent(long userId, long eventId, Instant timestamp) {
        collectUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW, timestamp);
    }

    public void sendRegisterEvent(long userId, long eventId, Instant timestamp) {
        collectUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER, timestamp);
    }

    public void sendLikeEvent(long userId, long eventId, Instant timestamp) {
        collectUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE, timestamp);
    }
}