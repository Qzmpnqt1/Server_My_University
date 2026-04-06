-- Тестовый глобальный администратор (тот же BCrypt, что в V15 для пароля Admin123!)
INSERT INTO users (email, password_hash, first_name, last_name, user_type, is_active)
VALUES (
    'superadmin@moyvuz.local',
    '$2a$10$EqKcp1WFKTmjMEkEz7MBQ.gldJXbPOiJFzGZLaEG9/JOC3KfKgjYC',
    'Супер',
    'Администратор',
    'SUPER_ADMIN',
    true
);

INSERT INTO admin_profiles (user_id, university_id, role)
SELECT id, NULL, 'SUPER_ADMIN' FROM users WHERE email = 'superadmin@moyvuz.local';
