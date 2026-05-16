package ru.practicum.stats.analyzer.projection;

public interface SimilarEventProjection {
    Long getOtherEventId();

    Double getScore();
}