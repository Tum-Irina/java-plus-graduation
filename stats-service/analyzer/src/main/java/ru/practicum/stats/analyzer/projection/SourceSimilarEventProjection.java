package ru.practicum.stats.analyzer.projection;

public interface SourceSimilarEventProjection {
    Long getSourceEventId();

    Long getOtherEventId();

    Double getScore();
}