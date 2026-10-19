-- GhostPocket Cloudflare D1 Database Schema
-- Mirrors local SQLite Room schema for 100% relational parity

CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    email TEXT,
    password_hash TEXT,
    display_name TEXT,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS categories (
    name TEXT NOT NULL,
    user_id TEXT NOT NULL,
    icon_key TEXT NOT NULL DEFAULT 'ghost',
    budget_paise INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (name, user_id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    amount_paise INTEGER NOT NULL,
    type TEXT NOT NULL,
    category TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    note TEXT,
    payment_method TEXT,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS friends (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL,
    phone TEXT,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS friend_ledgers (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    friend_id TEXT NOT NULL,
    amount_paise INTEGER NOT NULL,
    direction TEXT NOT NULL,
    note TEXT,
    timestamp INTEGER NOT NULL,
    is_settled INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_settings (
    id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    budget_paise INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (id, user_id)
);

-- Fast sync indices
CREATE INDEX IF NOT EXISTS idx_tx_user_updated ON transactions(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_friends_user_updated ON friends(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_ledgers_user_updated ON friend_ledgers(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_categories_user_updated ON categories(user_id, updated_at);
