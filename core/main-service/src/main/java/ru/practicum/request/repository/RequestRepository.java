package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;

import java.util.List;
import java.util.Set;

public interface RequestRepository extends JpaRepository<Request, Long> {
    // Поиск всех запросов пользователя
    List<Request> findAllByRequesterId(Long requesterId);

    // Подсчет количества подтвержденных заявок
    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    // Проверка на дубликаты
    Boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    // Получение списка заявок для конкретного события
    List<Request> findAllByEventId(Long eventId);

    // Получение списка заявок по списку их ID
    List<Request> findAllByIdIn(List<Long> ids);

    // Подсчет подтвержденных запросов по ID событий
    @Query("SELECT r.event.id, COUNT(r) " +
            "FROM Request r " +
            "WHERE r.event.id IN :eventIds " +
            "AND r.status = 'CONFIRMED' " +
            "GROUP BY r.event.id")
    List<Object[]> countConfirmedRequestsByEventIds(@Param("eventIds") Set<Long> eventIds);

    // Проверяем, что пользователь посетил событие (имел подтвержденную заявку)
    Boolean existsByRequesterIdAndEventIdAndStatus(Long requesterId, Long eventId, RequestStatus status);
}
