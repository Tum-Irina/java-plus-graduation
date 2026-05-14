package ru.practicum.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.analyzer.entity.EventSimilarityEntity;

import java.util.List;
import java.util.Optional;

public interface SimilarityRepository extends JpaRepository<EventSimilarityEntity, Long> {

    Optional<EventSimilarityEntity> findByEventAAndEventB(Long eventA, Long eventB);

    @Query("SELECT s FROM EventSimilarityEntity s WHERE s.eventA = :eventId OR s.eventB = :eventId")
    List<EventSimilarityEntity> findAllByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Transactional
    @Query("UPDATE EventSimilarityEntity s SET s.score = :score, s.updatedAt = :updatedAt " +
           "WHERE s.eventA = :eventA AND s.eventB = :eventB")
    int updateScore(@Param("eventA") Long eventA,
                    @Param("eventB") Long eventB,
                    @Param("score") Double score,
                    @Param("updatedAt") Long updatedAt);

    @Query("SELECT s FROM EventSimilarityEntity s WHERE s.eventA = :eventId OR s.eventB = :eventId ORDER BY s.score DESC")
    List<EventSimilarityEntity> findTopByEventIdOrderByScoreDesc(@Param("eventId") Long eventId);

    @Query(value = "SELECT CASE WHEN s.eventA = :eventId THEN s.eventB ELSE s.eventA END as other_event, s.score " +
           "FROM similarities s WHERE (s.eventA = :eventId OR s.eventB = :eventId) AND s.score > 0 " +
           "ORDER BY s.score DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopSimilarEvents(@Param("eventId") Long eventId, @Param("limit") int limit);
}