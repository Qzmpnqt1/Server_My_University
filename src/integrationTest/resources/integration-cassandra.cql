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
