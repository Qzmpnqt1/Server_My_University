package org.example.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subject_practices", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"subject_direction_id", "practice_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectPractice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_direction_id", nullable = false)
    private SubjectInDirection subjectDirection;

    @Column(name = "practice_number", nullable = false)
    private Integer practiceNumber;

    @Column(name = "practice_title", nullable = false)
    private String practiceTitle;

    @Column(name = "max_grade")
    private Integer maxGrade;

    @Column(name = "is_credit")
    private Boolean isCredit;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
