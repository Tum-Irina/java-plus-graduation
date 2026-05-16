package ru.practicum.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.stats.analyzer.entity.EventSimilarityEntity;
import ru.practicum.stats.analyzer.projection.SimilarEventProjection;
import ru.practicum.stats.analyzer.projection.SourceSimilarEventProjection;

import java.util.List;
import java.util.Optional;

public interface SimilarityRepository extends JpaRepository<EventSimilarityEntity, Long> {

    Optional<EventSimilarityEntity> findByEventAAndEventB(Long eventA, Long eventB);

    @Query("SELECT s FROM EventSimilarityEntity s WHERE s.eventA = :eventId OR s.eventB = :eventId")
    List<EventSimilarityEntity> findAllByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Query("UPDATE EventSimilarityEntity s SET s.score = :score, s.updatedAt = :updatedAt " +
           "WHERE s.eventA = :eventA AND s.eventB = :eventB")
    int updateScore(@Param("eventA") Long eventA,
                    @Param("eventB") Long eventB,
                    @Param("score") Double score,
                    @Param("updatedAt") Long updatedAt);

    @Query("SELECT s FROM EventSimilarityEntity s WHERE s.eventA = :eventId OR s.eventB = :eventId ORDER BY s.score DESC")
    List<EventSimilarityEntity> findTopByEventIdOrderByScoreDesc(@Param("eventId") Long eventId);

    @Query(value = "SELECT s.event_a as sourceEventId, " +
            "CASE WHEN s.event_a = :eventId THEN s.event_b ELSE s.event_a END as otherEventId, " +
            "s.score as score " +
            "FROM similarities s " +
            "WHERE (s.event_a IN (:eventIds) OR s.event_b IN (:eventIds)) " +
            "AND s.score > 0 " +
            "ORDER BY s.score DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<SourceSimilarEventProjection> findTopSimilarEventsForMultipleEvents(@Param("eventIds") List<Long> eventIds,
                                                                             @Param("limit") int limit);

    @Query(value = "SELECT CASE WHEN s.event_a = :eventId THEN s.event_b ELSE s.event_a END as otherEventId, " +
            "s.score as score " +
            "FROM similarities s " +
            "WHERE (s.event_a = :eventId OR s.event_b = :eventId) " +
            "AND s.score > 0 " +
            "AND CASE WHEN s.event_a = :eventId THEN s.event_b ELSE s.event_a END NOT IN (:excludedEventIds) " +
            "ORDER BY s.score DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<SimilarEventProjection> findTopSimilarEventsExcluding(@Param("eventId") Long eventId,
                                                               @Param("excludedEventIds") List<Long> excludedEventIds,
                                                               @Param("limit") int limit);

    @Query(value = "SELECT CASE WHEN s.event_a = :eventId THEN s.event_b ELSE s.event_a END as otherEventId, " +
            "s.score as score " +
            "FROM similarities s " +
            "WHERE (s.event_a = :eventId OR s.event_b = :eventId) AND s.score > 0 " +
            "ORDER BY s.score DESC LIMIT :limit", nativeQuery = true)
    List<SimilarEventProjection> findTopSimilarEvents(@Param("eventId") Long eventId, @Param("limit") int limit);
}