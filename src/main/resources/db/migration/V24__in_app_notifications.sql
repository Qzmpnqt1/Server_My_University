-- Внутриприложенческие уведомления пользователя (персистентные, для мобильного клиента и аудита UX).
CREATE TABLE in_app_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    kind VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_in_app_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    KEY idx_in_app_notif_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
