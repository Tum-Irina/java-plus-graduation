package ru.practicum.event.repository;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.querydsl.core.BooleanBuilder;
import org.springframework.data.support.PageableExecutionUtils;
import ru.practicum.event.Event;
import ru.practicum.event.QEvent;
import ru.practicum.event.SortEvents;
import ru.practicum.event.State;
import ru.practicum.event.params.AdminEventsParam;
import ru.practicum.event.params.PublicEventsParam;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;


public class EventRepositoryImpl implements EventRepositoryCustom {

    private final QEvent qEvent = QEvent.event;
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    public EventRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Page<Event> getEventsPublic(PublicEventsParam publicEventsParam, Pageable pageable) {
        BooleanBuilder predicate = new BooleanBuilder();

        predicate.and(qEvent.state.eq(State.PUBLISHED));

        if (publicEventsParam.getText() != null && !publicEventsParam.getText().isEmpty()) {
            predicate.and(qEvent.annotation.containsIgnoreCase(publicEventsParam.getText())
            .or(qEvent.description.containsIgnoreCase(publicEventsParam.getText())));
        }

        if (publicEventsParam.getCategories() != null) {
            predicate.and(qEvent.category.id.in(publicEventsParam.getCategories()));
        }

        if (publicEventsParam.getRangeStart() == null && publicEventsParam.getRangeEnd() == null) {
            predicate.and(qEvent.eventDate.after(LocalDateTime.now()));
        } else {
            if (publicEventsParam.getRangeStart() != null) {
                predicate.and(qEvent.eventDate.after(LocalDateTime.from(FORMATTER.parse(publicEventsParam.getRangeStart()))));
            }
            if (publicEventsParam.getRangeEnd() != null) {
                predicate.and(qEvent.eventDate.before(LocalDateTime.from(FORMATTER.parse(publicEventsParam.getRangeEnd()))));
            }
        }

        JPAQuery<Event> query = queryFactory
                .select(qEvent)
                .from(qEvent)
                .where(predicate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());
        if (publicEventsParam.getSort() == SortEvents.EVENT_DATE) {
            query.orderBy(qEvent.eventDate.desc());
        }
        List<Event> content = query.fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(qEvent.count())
                .from(qEvent)
                .where(predicate);

        return PageableExecutionUtils.getPage(content, pageable,
                countQuery::fetchOne);
    }

    @Override
    public Page<Event> searchEventsByAdmin(AdminEventsParam adminEventsParam, Pageable pageable) {
        BooleanBuilder predicate = new BooleanBuilder();

        if (adminEventsParam.getUsers() != null) {
            predicate.and(qEvent.initiator.id.in(adminEventsParam.getUsers()));
        }

        if (adminEventsParam.getStates() != null) {
            predicate.and(qEvent.state.in(adminEventsParam.getStates()));
        }

        if (adminEventsParam.getCategories() != null) {
            predicate.and(qEvent.category.id.in(adminEventsParam.getCategories()));
        }

        if (adminEventsParam.getRangeStart() == null &&  adminEventsParam.getRangeEnd() == null) {
            predicate.and(qEvent.eventDate.after(LocalDateTime.now()));
        } else {
            if (adminEventsParam.getRangeStart() != null) {
                predicate.and(qEvent.eventDate.after(LocalDateTime.from(FORMATTER.parse(adminEventsParam.getRangeStart()))));
            }
            if (adminEventsParam.getRangeEnd() != null) {
                predicate.and(qEvent.eventDate.before(LocalDateTime.from(FORMATTER.parse(adminEventsParam.getRangeEnd()))));
            }
        }
        List<Event> content = queryFactory
                .select(qEvent)
                .from(qEvent)
                .where(predicate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qEvent.eventDate.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(qEvent.count())
                .from(qEvent)
                .where(predicate);

        return PageableExecutionUtils.getPage(content, pageable,
                countQuery::fetchOne);
    }
}
