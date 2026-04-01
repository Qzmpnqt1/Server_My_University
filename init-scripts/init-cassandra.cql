CREATE KEYSPACE IF NOT EXISTS my_university_chat
WITH REPLICATION = {
    'class': 'SimpleStrategy',
    'replication_factor': 1
};

USE my_university_chat;

CREATE TABLE IF NOT EXISTS conversations_by_user (
    user_id bigint,
    conversation_id uuid,
    participant_id bigint,
    participant_name text,
    last_message_text text,
    last_message_at timestamp,
    unread_count int,
    PRIMARY KEY (user_id, last_message_at, conversation_id)
) WITH CLUSTERING ORDER BY (last_message_at DESC, conversation_id ASC);

CREATE TABLE IF NOT EXISTS messages_by_conversation (
    conversation_id uuid,
    message_id timeuuid,
    sender_id bigint,
    sender_name text,
    text text,
    sent_at timestamp,
    PRIMARY KEY (conversation_id, sent_at, message_id)
) WITH CLUSTERING ORDER BY (sent_at DESC, message_id DESC);

-- ===========================================================
-- Seed data: demo conversation between user 2 and user 3
-- conversation_id = UUID v3 from "2:3"
-- ===========================================================

INSERT INTO messages_by_conversation (conversation_id, message_id, sender_id, sender_name, text, sent_at)
VALUES (d3a1f4c8-9b2e-3d7a-8f1e-6b5c4a3d2e1f, now(), 2, 'Иванов Пётр', 'Привет! Когда следующая лабораторная?', '2025-09-01 10:00:00+0000');

INSERT INTO messages_by_conversation (conversation_id, message_id, sender_id, sender_name, text, sent_at)
VALUES (d3a1f4c8-9b2e-3d7a-8f1e-6b5c4a3d2e1f, now(), 3, 'Петрова Анна', 'Привет! В среду в 14:00, корпус 1, аудитория 205.', '2025-09-01 10:05:00+0000');

INSERT INTO messages_by_conversation (conversation_id, message_id, sender_id, sender_name, text, sent_at)
VALUES (d3a1f4c8-9b2e-3d7a-8f1e-6b5c4a3d2e1f, now(), 2, 'Иванов Пётр', 'Спасибо! Нужно подготовить отчёт?', '2025-09-01 10:07:00+0000');

INSERT INTO messages_by_conversation (conversation_id, message_id, sender_id, sender_name, text, sent_at)
VALUES (d3a1f4c8-9b2e-3d7a-8f1e-6b5c4a3d2e1f, now(), 3, 'Петрова Анна', 'Да, шаблон в системе. Удачи!', '2025-09-01 10:10:00+0000');

-- Conversation entries for user 2 (sees participant 3)
INSERT INTO conversations_by_user (user_id, conversation_id, participant_id, participant_name, last_message_text, last_message_at, unread_count)
VALUES (2, d3a1f4c8-9b2e-3d7a-8f1e-6b5c4a3d2e1f, 3, 'Петрова Анна', 'Да, шаблон в системе. Удачи!', '2025-09-01 10:10:00+0000', 1);

-- Conversation entries for user 3 (sees participant 2)
INSERT INTO conversations_by_user (user_id, conversation_id, participant_id, participant_name, last_message_text, last_message_at, unread_count)
VALUES (3, d3a1f4c8-9b2e-3d7a-8f1e-6b5c4a3d2e1f, 2, 'Иванов Пётр', 'Да, шаблон в системе. Удачи!', '2025-09-01 10:10:00+0000', 0);

-- Seed data: demo conversation between user 2 and admin (user 1)
-- conversation_id = UUID v3 from "1:2"

INSERT INTO messages_by_conversation (conversation_id, message_id, sender_id, sender_name, text, sent_at)
VALUES (a7b3c1d2-4e5f-3a6b-8c9d-0e1f2a3b4c5d, now(), 2, 'Иванов Пётр', 'Здравствуйте! Не могу войти в расписание.', '2025-09-02 09:00:00+0000');

INSERT INTO messages_by_conversation (conversation_id, message_id, sender_id, sender_name, text, sent_at)
VALUES (a7b3c1d2-4e5f-3a6b-8c9d-0e1f2a3b4c5d, now(), 1, 'Администратор Системный', 'Добрый день! Проверьте, пожалуйста, что вы привязаны к группе. Если нет — обратитесь в деканат.', '2025-09-02 09:15:00+0000');

INSERT INTO conversations_by_user (user_id, conversation_id, participant_id, participant_name, last_message_text, last_message_at, unread_count)
VALUES (2, a7b3c1d2-4e5f-3a6b-8c9d-0e1f2a3b4c5d, 1, 'Администратор Системный', 'Добрый день! Проверьте, пожалуйста, что вы привязаны к группе.', '2025-09-02 09:15:00+0000', 1);

INSERT INTO conversations_by_user (user_id, conversation_id, participant_id, participant_name, last_message_text, last_message_at, unread_count)
VALUES (1, a7b3c1d2-4e5f-3a6b-8c9d-0e1f2a3b4c5d, 2, 'Иванов Пётр', 'Добрый день! Проверьте, пожалуйста, что вы привязаны к группе.', '2025-09-02 09:15:00+0000', 0);
