-- Удаление исторически неконсистентных оценок: студент из группы с другим курсом или направлением,
-- чем у дисциплины (subject_in_direction). Соответствует бизнес-правилу CourseConsistency.
-- Версия 23: V22 зарезервирована под Java-миграцию V22__TeacherSubjectsSubjectInDirection.

DELETE pg FROM practice_grades pg
INNER JOIN student_profiles prof ON prof.user_id = pg.student_id
INNER JOIN academic_groups ag ON ag.id = prof.group_id
INNER JOIN subject_practices spr ON spr.id = pg.practice_id
INNER JOIN subjects_in_directions sid ON sid.id = spr.subject_direction_id
WHERE ag.direction_id <> sid.direction_id
   OR NOT (ag.course <=> sid.course);

DELETE g FROM grades g
INNER JOIN student_profiles prof ON prof.user_id = g.student_id
INNER JOIN academic_groups ag ON ag.id = prof.group_id
INNER JOIN subjects_in_directions sid ON sid.id = g.subject_direction_id
WHERE ag.direction_id <> sid.direction_id
   OR NOT (ag.course <=> sid.course);
