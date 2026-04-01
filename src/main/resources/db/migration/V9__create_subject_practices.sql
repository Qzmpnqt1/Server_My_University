CREATE TABLE subject_practices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_direction_id BIGINT NOT NULL,
    practice_number INT NOT NULL,
    practice_title VARCHAR(255) NOT NULL,
    max_grade INT,
    is_credit BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sp_subject_direction FOREIGN KEY (subject_direction_id) REFERENCES subjects_in_directions(id),
    UNIQUE KEY uk_sp_direction_number (subject_direction_id, practice_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
