CREATE TABLE teacher_subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ts_teacher FOREIGN KEY (teacher_id) REFERENCES teacher_profiles(id),
    CONSTRAINT fk_ts_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    UNIQUE KEY uk_ts_teacher_subject (teacher_id, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
