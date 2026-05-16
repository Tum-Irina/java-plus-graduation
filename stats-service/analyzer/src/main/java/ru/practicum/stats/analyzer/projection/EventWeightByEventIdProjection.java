package ru.practicum.stats.analyzer.projection;

public interface EventWeightByEventIdProjection {
    Long getEventId();

    Double getWeight();
}