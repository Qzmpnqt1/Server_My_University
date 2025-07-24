-- Create keyspace for the chat functionality
CREATE KEYSPACE IF NOT EXISTS my_university_chat
WITH REPLICATION = {
    'class': 'SimpleStrategy',
    'replication_factor': 1
};

-- Use the keyspace
USE my_university_chat;

-- Create chats table
CREATE TABLE IF NOT EXISTS chats (
    chat_id uuid,
    user_id uuid,
    is_group boolean,
    chat_name text,
    created_at timestamp,
    PRIMARY KEY (user_id, chat_id)
);

-- Create messages table
CREATE TABLE IF NOT EXISTS messages (
    chat_id uuid,
    message_id timeuuid,
    sender_id uuid,
    content text,
    is_read boolean,
    PRIMARY KEY (chat_id, message_id)
) WITH CLUSTERING ORDER BY (message_id DESC);

-- Create chat participants table
CREATE TABLE IF NOT EXISTS chat_participants (
    chat_id uuid,
    user_id uuid,
    joined_at timestamp,
    is_admin boolean,
    PRIMARY KEY (chat_id, user_id)
);

-- Create user chats table with counter
CREATE TABLE IF NOT EXISTS user_chats (
    user_id uuid,
    chat_id uuid,
    last_read_message_id timeuuid,
    unread_count counter,
    PRIMARY KEY (user_id, chat_id)
);

-- Create indexes for faster lookups
CREATE INDEX IF NOT EXISTS idx_chat_participants_user_id ON chat_participants (user_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages (sender_id); 