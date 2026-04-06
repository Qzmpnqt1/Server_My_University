-- Привязка преподавателя к вузу без обязательного института (институты задаются через назначения).
ALTER TABLE teacher_profiles
    ADD COLUMN university_id BIGINT NULL,
    ADD CONSTRAINT fk_teacher_profiles_university
        FOREIGN KEY (university_id) REFERENCES universities (id);

UPDATE teacher_profiles tp
    INNER JOIN institutes i ON tp.institute_id = i.id
SET tp.university_id = i.university_id
WHERE tp.institute_id IS NOT NULL
  AND tp.university_id IS NULL;
