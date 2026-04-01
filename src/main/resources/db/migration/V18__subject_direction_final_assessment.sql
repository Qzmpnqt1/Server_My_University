-- Тип итоговой аттестации по предмету в направлении: экзамен (оценка 2–5) или зачёт (credit_status)
ALTER TABLE subjects_in_directions
    ADD COLUMN final_assessment_type VARCHAR(20) NOT NULL DEFAULT 'EXAM'
        COMMENT 'EXAM | CREDIT';

-- Нормализация роли администратора в профилях
UPDATE admin_profiles SET role = 'ADMIN' WHERE role IS NOT NULL AND role <> 'ADMIN';
