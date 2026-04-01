CREATE TABLE grades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_direction_id BIGINT NOT NULL,
    grade INT,
    credit_status BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_grades_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_grades_subject_direction FOREIGN KEY (subject_direction_id) REFERENCES subjects_in_directions(id),
    UNIQUE KEY uk_grades_student_subject (student_id, subject_direction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE practice_grades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    practice_id BIGINT NOT NULL,
    grade INT,
    credit_status BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pg_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_pg_practice FOREIGN KEY (practice_id) REFERENCES subject_practices(id),
    UNIQUE KEY uk_pg_student_practice (student_id, practice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
