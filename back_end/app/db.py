import sqlite3
from datetime import datetime, timezone

from app.config import (
    DATA_DIR,
    DB_PATH,
    DEMO_DEVICE_KEY,
    DEMO_DEVICE_NAME,
    DEMO_EMAIL,
    DEMO_NAME,
    DEMO_PASSWORD,
)
from app.security import hash_password


def connect() -> sqlite3.Connection:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def utcnow() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def init_db() -> None:
    with connect() as conn:
        conn.executescript(
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                created_at TEXT NOT NULL
            );

            CREATE TABLE IF NOT EXISTS devices (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                name TEXT NOT NULL,
                api_key TEXT NOT NULL UNIQUE,
                last_seen TEXT,
                created_at TEXT NOT NULL
            );

            CREATE TABLE IF NOT EXISTS readings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                device_id INTEGER NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
                temperature REAL,
                humidity REAL,
                message TEXT,
                created_at TEXT NOT NULL
            );

            CREATE INDEX IF NOT EXISTS idx_readings_device_time
                ON readings(device_id, id DESC);
            """
        )
        row = conn.execute(
            "SELECT id FROM users WHERE email = ?", (DEMO_EMAIL,)
        ).fetchone()
        if row is None:
            now = utcnow()
            cur = conn.execute(
                "INSERT INTO users (email, name, password_hash, created_at) VALUES (?, ?, ?, ?)",
                (DEMO_EMAIL, DEMO_NAME, hash_password(DEMO_PASSWORD), now),
            )
            user_id = cur.lastrowid
            conn.execute(
                """
                INSERT INTO devices (user_id, name, api_key, created_at)
                VALUES (?, ?, ?, ?)
                """,
                (user_id, DEMO_DEVICE_NAME, DEMO_DEVICE_KEY, now),
            )
        conn.commit()
