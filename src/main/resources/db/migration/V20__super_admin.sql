-- Глобальный супер-администратор и опциональная привязка admin_profiles к вузу
ALTER TABLE users
    MODIFY COLUMN user_type ENUM('ADMIN', 'STUDENT', 'TEACHER', 'SUPER_ADMIN') NOT NULL;

ALTER TABLE admin_profiles
    MODIFY COLUMN university_id BIGINT NULL;
