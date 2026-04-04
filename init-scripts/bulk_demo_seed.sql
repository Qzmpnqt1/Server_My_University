-- =============================================================================
-- Демо-данные для MySQL (my_university)
-- Выполнять ОДИН РАЗ после Flyway, на базе с начальным seed V15.
-- Пароль у всех новых пользователей: Admin123! (тот же BCrypt, что у admin@university.ru)
-- Повторный запуск даст ошибку уникальности email — сначала удалите строки с *@demo.ru
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------------------------- вузы, институты, направления, группы
INSERT INTO universities (id, name, short_name, city) VALUES
  (2, 'Санкт-Петербургский политехнический университет', 'СПбПУ', 'Санкт-Петербург'),
  (3, 'Казанский (Приволжский) федеральный университет', 'КФУ', 'Казань');

INSERT INTO institutes (id, name, short_name, university_id) VALUES
  (2, 'Физико-технический институт', 'ФТИ', 2),
  (3, 'Институт компьютерных наук и технологий', 'ИКНиТ', 2),
  (4, 'Институт физики', 'ИФ', 3);

INSERT INTO study_directions (id, name, code, institute_id) VALUES
  (2, 'Прикладная математика и информатика', '01.03.02', 2),
  (3, 'Информатика и вычислительная техника', '09.03.01', 3),
  (4, 'Физика', '03.03.02', 4),
  (5, 'Информационные системы и технологии', '09.03.03', 1);

INSERT INTO academic_groups (id, name, course, year_of_admission, direction_id) VALUES
  (3, 'ПМ-21', 3, 2021, 2),
  (4, 'ИВТ-22', 2, 2022, 3),
  (5, 'ФИЗ-20', 4, 2020, 4),
  (6, 'ИС-21', 3, 2021, 5),
  (7, 'ПИ-23', 1, 2023, 1);

-- --------------------------------------------------------------------------- предметы и связи с направлениями
INSERT INTO subjects (id, name) VALUES
  (4, 'Алгоритмы и структуры данных'),
  (5, 'Операционные системы'),
  (6, 'Теория вероятностей'),
  (7, 'Квантовая механика'),
  (8, 'Веб-разработка');

INSERT INTO subjects_in_directions (id, subject_id, direction_id, course, semester) VALUES
  (4, 4, 2, 3, 5),
  (5, 5, 3, 2, 3),
  (6, 6, 4, 3, 1),
  (7, 4, 5, 2, 3),
  (8, 8, 1, 2, 4),
  (9, 7, 4, 3, 2);

INSERT INTO subject_lesson_types (id, subject_direction_id, lesson_type) VALUES
  (5, 4, 'LECTURE'),
  (6, 4, 'LABORATORY'),
  (7, 5, 'LECTURE'),
  (8, 6, 'SEMINAR'),
  (9, 7, 'LECTURE'),
  (10, 8, 'LABORATORY');

INSERT INTO subject_practices (id, subject_direction_id, practice_number, practice_title, max_grade, is_credit) VALUES
  (1, 4, 1, 'Лабораторная: сортировки', 10, FALSE),
  (2, 4, 2, 'Лабораторная: графы', 10, FALSE),
  (3, 5, 1, 'Зачётная работа ОС', NULL, TRUE);

-- --------------------------------------------------------------------------- аудитории во 2-м и 3-м вузах
INSERT INTO classrooms (id, building, room_number, capacity, university_id) VALUES
  (4, 'Главный корпус', '12-15', 40, 2),
  (5, 'УЛК', '3-201', 60, 2),
  (6, 'Физический факультет', '7-101', 35, 3);

SET FOREIGN_KEY_CHECKS = 1;

-- BCrypt для пароля Admin123! (как в V15)
SET @pwd := '$2a$10$EqKcp1WFKTmjMEkEz7MBQ.gldJXbPOiJFzGZLaEG9/JOC3KfKgjYC';

-- --------------------------------------------------------------------------- пользователи: 3 админа (id 2–4), 20 преподавателей (5–24), 20 студентов (25–44)
INSERT INTO users (id, email, password_hash, first_name, last_name, middle_name, user_type, is_active) VALUES
  (2, 'admin.spb@demo.ru', @pwd, 'Ольга', 'Соколова', 'Игоревна', 'ADMIN', TRUE),
  (3, 'admin.kzn@demo.ru', @pwd, 'Руслан', 'Хасанов', 'Тагирович', 'ADMIN', TRUE),
  (4, 'admin.msu2@demo.ru', @pwd, 'Елена', 'Волкова', 'Сергеевна', 'ADMIN', TRUE),
  (5, 'teacher01@demo.ru', @pwd, 'Иван', 'Петров', 'Сергеевич', 'TEACHER', TRUE),
  (6, 'teacher02@demo.ru', @pwd, 'Мария', 'Сидорова', 'Андреевна', 'TEACHER', TRUE),
  (7, 'teacher03@demo.ru', @pwd, 'Алексей', 'Козлов', NULL, 'TEACHER', TRUE),
  (8, 'teacher04@demo.ru', @pwd, 'Наталья', 'Морозова', 'Павловна', 'TEACHER', TRUE),
  (9, 'teacher05@demo.ru', @pwd, 'Дмитрий', 'Новиков', 'Ильич', 'TEACHER', TRUE),
  (10, 'teacher06@demo.ru', @pwd, 'Анна', 'Фёдорова', NULL, 'TEACHER', TRUE),
  (11, 'teacher07@demo.ru', @pwd, 'Сергей', 'Лебедев', 'Викторович', 'TEACHER', TRUE),
  (12, 'teacher08@demo.ru', @pwd, 'Татьяна', 'Егорова', 'Николаевна', 'TEACHER', TRUE),
  (13, 'teacher09@demo.ru', @pwd, 'Павел', 'Семёнов', NULL, 'TEACHER', TRUE),
  (14, 'teacher10@demo.ru', @pwd, 'Екатерина', 'Голубева', 'Дмитриевна', 'TEACHER', TRUE),
  (15, 'teacher11@demo.ru', @pwd, 'Михаил', 'Виноградов', 'Олегович', 'TEACHER', TRUE),
  (16, 'teacher12@demo.ru', @pwd, 'Оксана', 'Богданова', NULL, 'TEACHER', TRUE),
  (17, 'teacher13@demo.ru', @pwd, 'Андрей', 'Фомин', 'Романович', 'TEACHER', TRUE),
  (18, 'teacher14@demo.ru', @pwd, 'Юлия', 'Рыбакова', 'Евгеньевна', 'TEACHER', TRUE),
  (19, 'teacher15@demo.ru', @pwd, 'Константин', 'Зайцев', NULL, 'TEACHER', TRUE),
  (20, 'teacher16@demo.ru', @pwd, 'Виктория', 'Соловьёва', 'Игоревна', 'TEACHER', TRUE),
  (21, 'teacher17@demo.ru', @pwd, 'Николай', 'Жуков', 'Петрович', 'TEACHER', TRUE),
  (22, 'teacher18@demo.ru', @pwd, 'Ирина', 'Комарова', NULL, 'TEACHER', TRUE),
  (23, 'teacher19@demo.ru', @pwd, 'Владимир', 'Орлов', 'Станиславович', 'TEACHER', TRUE),
  (24, 'teacher20@demo.ru', @pwd, 'Светлана', 'Макарова', 'Юрьевна', 'TEACHER', TRUE),
  (25, 'student01@demo.ru', @pwd, 'Артём', 'Алексеев', 'Павлович', 'STUDENT', TRUE),
  (26, 'student02@demo.ru', @pwd, 'Дарья', 'Белова', NULL, 'STUDENT', TRUE),
  (27, 'student03@demo.ru', @pwd, 'Максим', 'Воробьёв', 'Денисович', 'STUDENT', TRUE),
  (28, 'student04@demo.ru', @pwd, 'Полина', 'Григорьева', 'Ивановна', 'STUDENT', TRUE),
  (29, 'student05@demo.ru', @pwd, 'Кирилл', 'Давыдов', NULL, 'STUDENT', TRUE),
  (30, 'student06@demo.ru', @pwd, 'София', 'Журавлёва', 'Максимовна', 'STUDENT', TRUE),
  (31, 'student07@demo.ru', @pwd, 'Тимофей', 'Захаров', 'Артёмович', 'STUDENT', TRUE),
  (32, 'student08@demo.ru', @pwd, 'Вероника', 'Ильина', NULL, 'STUDENT', TRUE),
  (33, 'student09@demo.ru', @pwd, 'Глеб', 'Киселёв', 'Сергеевич', 'STUDENT', TRUE),
  (34, 'student10@demo.ru', @pwd, 'Алиса', 'Ларионова', 'Олеговна', 'STUDENT', TRUE),
  (35, 'student11@demo.ru', @pwd, 'Роман', 'Мельников', NULL, 'STUDENT', TRUE),
  (36, 'student12@demo.ru', @pwd, 'Милана', 'Никифорова', 'Тимуровна', 'STUDENT', TRUE),
  (37, 'student13@demo.ru', @pwd, 'Степан', 'Одинцов', 'Ильич', 'STUDENT', TRUE),
  (38, 'student14@demo.ru', @pwd, 'Ева', 'Панова', NULL, 'STUDENT', TRUE),
  (39, 'student15@demo.ru', @pwd, 'Матвей', 'Романов', 'Кириллович', 'STUDENT', TRUE),
  (40, 'student16@demo.ru', @pwd, 'Варвара', 'Степанова', 'Дмитриевна', 'STUDENT', TRUE),
  (41, 'student17@demo.ru', @pwd, 'Ярослав', 'Титов', NULL, 'STUDENT', TRUE),
  (42, 'student18@demo.ru', @pwd, 'Александра', 'Уварова', 'Николаевна', 'STUDENT', TRUE),
  (43, 'student19@demo.ru', @pwd, 'Даниил', 'Чернов', 'Максимович', 'STUDENT', TRUE),
  (44, 'student20@demo.ru', @pwd, 'Ксения', 'Шестакова', NULL, 'STUDENT', TRUE);

-- --------------------------------------------------------------------------- профили админов (по одному на вуз 2 и 3 + второй по МГУ)
INSERT INTO admin_profiles (user_id, university_id, role) VALUES
  (2, 2, 'ADMIN'),
  (3, 3, 'ADMIN'),
  (4, 1, 'ADMIN');

-- --------------------------------------------------------------------------- профили преподавателей (teacher_profiles.id = 1..20 для user 5..24)
-- institute_id чередуем между институтами 1–4
INSERT INTO teacher_profiles (id, user_id, institute_id, position) VALUES
  (1, 5, 1, 'Профессор'),
  (2, 6, 2, 'Доцент'),
  (3, 7, 3, 'Старший преподаватель'),
  (4, 8, 4, 'Доцент'),
  (5, 9, 1, 'Преподаватель'),
  (6, 10, 2, 'Профессор'),
  (7, 11, 3, 'Доцент'),
  (8, 12, 4, 'Старший преподаватель'),
  (9, 13, 1, 'Доцент'),
  (10, 14, 2, 'Преподаватель'),
  (11, 15, 3, 'Профессор'),
  (12, 16, 4, 'Доцент'),
  (13, 17, 1, 'Старший преподаватель'),
  (14, 18, 2, 'Доцент'),
  (15, 19, 3, 'Преподаватель'),
  (16, 20, 4, 'Профессор'),
  (17, 21, 1, 'Доцент'),
  (18, 22, 2, 'Старший преподаватель'),
  (19, 23, 3, 'Преподаватель'),
  (20, 24, 4, 'Доцент');

-- --------------------------------------------------------------------------- студенты: группы 1..7 по кругу, institute_id согласован с направлением группы
-- (group_id, institute_id): 1->1, 2->1, 3->2, 4->3, 5->4, 6->1, 7->1
INSERT INTO student_profiles (user_id, group_id, institute_id) VALUES
  (25, 1, 1), (26, 2, 1), (27, 3, 2), (28, 4, 3), (29, 5, 4), (30, 6, 1), (31, 7, 1),
  (32, 1, 1), (33, 2, 1), (34, 3, 2), (35, 4, 3), (36, 5, 4), (37, 6, 1), (38, 7, 1),
  (39, 1, 1), (40, 2, 1), (41, 3, 2), (42, 4, 3), (43, 5, 4), (44, 6, 1);

-- --------------------------------------------------------------------------- назначение преподавателей (teacher_id = teacher_profiles.id, subject_direction_id = subjects_in_directions.id)
INSERT INTO teacher_subjects (teacher_id, subject_direction_id) VALUES
  (1, 1), (1, 2), (2, 2), (2, 4), (3, 3), (3, 5), (4, 6), (4, 9), (5, 1), (5, 8),
  (6, 2), (7, 3), (8, 4), (9, 5), (10, 6), (11, 9), (12, 8), (13, 1), (14, 2), (15, 3),
  (16, 4), (17, 5), (18, 6), (19, 9), (20, 8);

-- --------------------------------------------------------------------------- оценки (часть студентов по существующим sid 1–3 и новым 4–5)
INSERT INTO grades (student_id, subject_direction_id, grade, credit_status) VALUES
  (25, 1, 5, NULL), (25, 2, 4, NULL), (26, 1, 4, NULL), (26, 3, NULL, TRUE),
  (27, 2, 5, NULL), (28, 1, 3, NULL), (29, 4, 8, NULL), (30, 5, 9, NULL),
  (31, 6, NULL, TRUE), (32, 7, 7, NULL), (33, 8, 8, NULL), (34, 1, 5, NULL);

INSERT INTO practice_grades (student_id, practice_id, grade, credit_status) VALUES
  (25, 1, 9, NULL), (25, 2, 8, NULL), (26, 1, 10, NULL), (27, 3, NULL, TRUE);

-- --------------------------------------------------------------------------- фрагмент расписания (преподаватель user id 5, тип занятия 1, группа 1, ауд. 1)
INSERT INTO schedule (subject_type_id, teacher_id, group_id, classroom_id, day_of_week, start_time, end_time, week_number) VALUES
  (1, 5, 1, 1, 1, '09:00:00', '10:30:00', 1),
  (2, 5, 1, 1, 1, '10:45:00', '12:15:00', 1),
  (3, 6, 2, 2, 2, '11:00:00', '12:30:00', 1),
  (7, 7, 3, 4, 3, '14:00:00', '15:30:00', 2);

-- --------------------------------------------------------------------------- счётчики AUTO_INCREMENT (чтобы следующие INSERT без id не конфликтовали)
ALTER TABLE universities AUTO_INCREMENT = 4;
ALTER TABLE institutes AUTO_INCREMENT = 5;
ALTER TABLE study_directions AUTO_INCREMENT = 6;
ALTER TABLE academic_groups AUTO_INCREMENT = 8;
ALTER TABLE subjects AUTO_INCREMENT = 9;
ALTER TABLE subjects_in_directions AUTO_INCREMENT = 10;
ALTER TABLE subject_lesson_types AUTO_INCREMENT = 11;
ALTER TABLE subject_practices AUTO_INCREMENT = 4;
ALTER TABLE classrooms AUTO_INCREMENT = 7;
ALTER TABLE users AUTO_INCREMENT = 45;
ALTER TABLE teacher_profiles AUTO_INCREMENT = 21;
ALTER TABLE student_profiles AUTO_INCREMENT = 21;
ALTER TABLE grades AUTO_INCREMENT = 20;
ALTER TABLE practice_grades AUTO_INCREMENT = 10;
ALTER TABLE schedule AUTO_INCREMENT = 10;
ALTER TABLE teacher_subjects AUTO_INCREMENT = 30;
