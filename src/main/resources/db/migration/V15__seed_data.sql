-- Seed university
INSERT INTO universities (name, short_name, city) VALUES ('Московский Государственный Университет', 'МГУ', 'Москва');

-- Seed institute
INSERT INTO institutes (name, short_name, university_id) VALUES ('Институт Информационных Технологий', 'ИИТ', 1);

-- Seed study direction
INSERT INTO study_directions (name, code, institute_id) VALUES ('Программная инженерия', '09.03.04', 1);

-- Seed academic groups
INSERT INTO academic_groups (name, course, year_of_admission, direction_id) VALUES ('ПИ-21', 3, 2021, 1);
INSERT INTO academic_groups (name, course, year_of_admission, direction_id) VALUES ('ПИ-22', 2, 2022, 1);

-- Seed admin user (password: Admin123!)
-- BCrypt hash generated for "Admin123!" with cost factor 10
INSERT INTO users (email, password_hash, first_name, last_name, user_type, is_active)
VALUES ('admin@university.ru', '$2a$10$EqKcp1WFKTmjMEkEz7MBQ.gldJXbPOiJFzGZLaEG9/JOC3KfKgjYC', 'Системный', 'Администратор', 'ADMIN', true);

INSERT INTO admin_profiles (user_id, university_id, role) VALUES (1, 1, 'ADMIN');

-- Seed subjects
INSERT INTO subjects (name) VALUES ('Базы данных');
INSERT INTO subjects (name) VALUES ('Программирование');
INSERT INTO subjects (name) VALUES ('Математический анализ');

-- Seed subjects in directions
INSERT INTO subjects_in_directions (subject_id, direction_id, course, semester) VALUES (1, 1, 3, 5);
INSERT INTO subjects_in_directions (subject_id, direction_id, course, semester) VALUES (2, 1, 2, 3);
INSERT INTO subjects_in_directions (subject_id, direction_id, course, semester) VALUES (3, 1, 1, 1);

-- Seed subject lesson types
INSERT INTO subject_lesson_types (subject_direction_id, lesson_type) VALUES (1, 'LECTURE');
INSERT INTO subject_lesson_types (subject_direction_id, lesson_type) VALUES (1, 'LABORATORY');
INSERT INTO subject_lesson_types (subject_direction_id, lesson_type) VALUES (2, 'LECTURE');
INSERT INTO subject_lesson_types (subject_direction_id, lesson_type) VALUES (2, 'SEMINAR');

-- Seed classroom
INSERT INTO classrooms (building, room_number, capacity, university_id) VALUES ('Корпус 1', '101', 100, 1);
INSERT INTO classrooms (building, room_number, capacity, university_id) VALUES ('Корпус 1', '205', 30, 1);
INSERT INTO classrooms (building, room_number, capacity, university_id) VALUES ('Корпус 2', '301', 25, 1);
