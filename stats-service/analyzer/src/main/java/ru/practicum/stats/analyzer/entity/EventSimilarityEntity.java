package ru.practicum.stats.analyzer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "similarities", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_a", "event_b"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSimilarityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_a", nullable = false)
    private Long eventA;

    @Column(name = "event_b", nullable = false)
    private Long eventB;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;
}