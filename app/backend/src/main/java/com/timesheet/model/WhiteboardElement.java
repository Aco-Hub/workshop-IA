package com.timesheet.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "whiteboard_elements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhiteboardElement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private WhiteboardRoom room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ElementType type;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String data;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String color = "#000000";

    @Column(name = "stroke_width", nullable = false)
    @Builder.Default
    private Integer strokeWidth = 2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Developer createdBy;

    @Column(name = "z_index", nullable = false)
    @Builder.Default
    private Integer zIndex = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        final LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
