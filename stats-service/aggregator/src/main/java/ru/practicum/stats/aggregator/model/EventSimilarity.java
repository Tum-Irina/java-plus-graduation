package ru.practicum.stats.aggregator.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventSimilarity {
    private long eventA;
    private long eventB;
    private double score;
}