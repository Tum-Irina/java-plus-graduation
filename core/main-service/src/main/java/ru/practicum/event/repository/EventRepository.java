package ru.practicum.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.practicum.event.Event;

import java.util.List;
import java.util.Set;

public interface EventRepository extends
        JpaRepository<Event, Long>,
        QuerydslPredicateExecutor<Event>,
        EventRepositoryCustom {

    Page<Event> findByInitiatorId(Long initiatorId, Pageable pageable);

    @Query("select e " +
            "from Event e " +
            "where e.id =  ?1 " +
            "and e.state = 'PUBLISHED'")
    Event findByIdPublished(Long eventId);

    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN FETCH e.category " +
            "LEFT JOIN FETCH e.initiator " +
            "WHERE e.id IN :ids")
    List<Event> findAllByIdIn(@Param("ids") Set<Long> ids);
}
