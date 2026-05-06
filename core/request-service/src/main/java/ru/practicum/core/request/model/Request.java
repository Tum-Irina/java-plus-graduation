package ru.practicum.core.request.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {
    // ID запроса
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Дата и время создания
    @Column(nullable = false)
    private LocalDateTime created;

    // Событие
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    // Запрашивающий пользователь
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    // Статус
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;
}
