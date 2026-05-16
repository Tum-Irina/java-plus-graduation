package ru.practicum.stats.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

import java.util.EnumMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ActionWeights {

    public static final double VIEW_WEIGHT = 0.4;
    public static final double REGISTER_WEIGHT = 0.8;
    public static final double LIKE_WEIGHT = 1.0;

    private static final Map<ActionTypeAvro, Double> WEIGHT_MAP = new EnumMap<>(ActionTypeAvro.class);

    static {
        WEIGHT_MAP.put(ActionTypeAvro.VIEW, VIEW_WEIGHT);
        WEIGHT_MAP.put(ActionTypeAvro.REGISTER, REGISTER_WEIGHT);
        WEIGHT_MAP.put(ActionTypeAvro.LIKE, LIKE_WEIGHT);
    }

    public static double getWeight(ActionTypeAvro actionType) {
        Double weight = WEIGHT_MAP.get(actionType);
        if (weight == null) {
            throw new IllegalArgumentException("Unknown action type: " + actionType);
        }
        return weight;
    }
}