CREATE TABLE schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_type_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    classroom_id BIGINT NOT NULL,
    day_of_week INT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    week_number INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedule_subject_type FOREIGN KEY (subject_type_id) REFERENCES subject_lesson_types(id),
    CONSTRAINT fk_schedule_teacher FOREIGN KEY (teacher_id) REFERENCES users(id),
    CONSTRAINT fk_schedule_group FOREIGN KEY (group_id) REFERENCES academic_groups(id),
    CONSTRAINT fk_schedule_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
