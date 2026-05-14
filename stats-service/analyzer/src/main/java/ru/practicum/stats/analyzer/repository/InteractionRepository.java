package ru.practicum.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.analyzer.entity.Interaction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    Optional<Interaction> findByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT i.eventId FROM Interaction i WHERE i.userId = :userId")
    Set<Long> findEventIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT i.eventId, i.weight FROM Interaction i WHERE i.userId = :userId")
    List<Object[]> findEventIdsAndWeightsByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Interaction i SET i.weight = :weight, i.updatedAt = :updatedAt WHERE i.userId = :userId AND i.eventId = :eventId")
    int updateWeight(@Param("userId") Long userId,
                     @Param("eventId") Long eventId,
                     @Param("weight") Double weight,
                     @Param("updatedAt") Long updatedAt);

    @Query("SELECT i.eventId, i.weight FROM Interaction i WHERE i.eventId IN :eventIds")
    List<Object[]> findWeightsByEventIds(@Param("eventIds") Set<Long> eventIds);
}