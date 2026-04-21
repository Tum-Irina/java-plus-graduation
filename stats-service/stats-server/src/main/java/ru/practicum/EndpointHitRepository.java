package ru.practicum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {

    @Query("select new ru.practicum.ViewStatsDto(eh.app, eh.uri, count(eh.ip)) " +
            "from EndpointHit eh " +
            "where (function('COALESCE', ?1, current_timestamp) = current_timestamp or eh.timestamp >= ?1) " +
            "and (function('COALESCE', ?2, current_timestamp) = current_timestamp or eh.timestamp <= ?2)" +
            "and (?3 is null or eh.uri in ?3) " +
            "group by eh.app, eh.uri " +
            "order by count(eh.ip) desc")
    List<ViewStatsDto> findStatistics(Instant start, Instant end, String[] uris);

    @Query("select new ru.practicum.ViewStatsDto(eh.app, eh.uri, count(distinct eh.ip)) " +
            "from EndpointHit eh " +
            "where (function('COALESCE', ?1, current_timestamp) = current_timestamp or eh.timestamp >= ?1) " +
            "and (function('COALESCE', ?2, current_timestamp) = current_timestamp or eh.timestamp <= ?2)" +
            "and (?3 is null or eh.uri in ?3) " +
            "group by eh.app, eh.uri " +
            "order by count(distinct eh.ip) desc")
    List<ViewStatsDto> findStatisticsUniqueIp(Instant start, Instant end, String[] uris);

    EndpointHit save(EndpointHit endpointHit);

    boolean existsById(Long id);
}