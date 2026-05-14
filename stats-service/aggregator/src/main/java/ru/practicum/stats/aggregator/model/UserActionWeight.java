package ru.practicum.stats.aggregator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserActionWeight {
    private long userId;
    private long eventId;
    private double weight;
    private long timestamp;
}