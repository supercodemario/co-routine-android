import asyncio
import random
import sqlite3
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from app.bus import bus
from app.config import ONLINE_SECONDS, TICK_SECONDS
from app.db import connect, utcnow
from app.deps import current_user, device_from_key, sse

router = APIRouter()


class RegisterBody(BaseModel):
    name: str = Field(min_length=1, max_length=80)
    email: str = Field(min_length=3, max_length=120)
    password: str = Field(min_length=6, max_length=128)


class LoginBody(BaseModel):
    email: str
    password: str


class DeviceBody(BaseModel):
    name: str = Field(min_length=1, max_length=80)


class TelemetryBody(BaseModel):
    temperature: Optional[float] = None
    humidity: Optional[float] = None
    message: Optional[str] = Field(default=None, max_length=280)


def _reading_event(device: dict, reading: dict) -> dict:
    return {
        "type": "telemetry",
        "device_id": device["id"],
        "device_name": device["name"],
        "user_id": device["user_id"],
        "temperature": reading["temperature"],
        "humidity": reading["humidity"],
        "message": reading["message"],
        "created_at": reading["created_at"],
    }


def _save_reading(device: dict, body: TelemetryBody) -> dict:
    now = utcnow()
    with connect() as conn:
        cur = conn.execute(
            """
            INSERT INTO readings (device_id, temperature, humidity, message, created_at)
            VALUES (?, ?, ?, ?, ?)
            """,
            (device["id"], body.temperature, body.humidity, body.message, now),
        )
        conn.execute(
            "UPDATE devices SET last_seen = ? WHERE id = ?",
            (now, device["id"]),
        )
        conn.commit()
        reading_id = cur.lastrowid
    reading = {
        "id": reading_id,
        "temperature": body.temperature,
        "humidity": body.humidity,
        "message": body.message,
        "created_at": now,
    }
    bus.publish(_reading_event(device, reading))
    return reading


@router.post("/auth/register")
def register(body: RegisterBody):
    from app.security import create_token, hash_password

    email = body.email.strip().lower()
    now = utcnow()
    try:
        with connect() as conn:
            cur = conn.execute(
                "INSERT INTO users (email, name, password_hash, created_at) VALUES (?, ?, ?, ?)",
                (email, body.name.strip(), hash_password(body.password), now),
            )
            conn.commit()
            user_id = cur.lastrowid
    except sqlite3.IntegrityError:
        raise HTTPException(status_code=409, detail="Email already registered")
    return {
        "token": create_token(user_id, email),
        "user": {"id": user_id, "email": email, "name": body.name.strip()},
    }


@router.post("/auth/login")
def login(body: LoginBody):
    from app.security import create_token, verify_password

    email = body.email.strip().lower()
    with connect() as conn:
        row = conn.execute(
            "SELECT id, email, name, password_hash FROM users WHERE email = ?",
            (email,),
        ).fetchone()
    if row is None or not verify_password(body.password, row["password_hash"]):
        raise HTTPException(status_code=401, detail="Wrong email or password")
    return {
        "token": create_token(row["id"], row["email"]),
        "user": {"id": row["id"], "email": row["email"], "name": row["name"]},
    }


@router.get("/auth/me")
def me(user: dict = Depends(current_user)):
    return user


@router.get("/dashboard")
def dashboard(user: dict = Depends(current_user)):
    with connect() as conn:
        devices = conn.execute(
            """
            SELECT id, name, last_seen, created_at
            FROM devices
            WHERE user_id = ?
            ORDER BY id
            """,
            (user["id"],),
        ).fetchall()
        readings = conn.execute(
            """
            SELECT r.id, r.device_id, d.name AS device_name,
                   r.temperature, r.humidity, r.message, r.created_at
            FROM readings r
            JOIN devices d ON d.id = r.device_id
            WHERE d.user_id = ?
            ORDER BY r.id DESC
            LIMIT 30
            """,
            (user["id"],),
        ).fetchall()
        counts = conn.execute(
            """
            SELECT COUNT(r.id) AS reading_count
            FROM readings r
            JOIN devices d ON d.id = r.device_id
            WHERE d.user_id = ?
            """,
            (user["id"],),
        ).fetchone()

    device_cards = []
    online = 0
    for device in devices:
        latest = next(
            (dict(r) for r in readings if r["device_id"] == device["id"]),
            None,
        )
        is_online = _is_online(device["last_seen"])
        online += int(is_online)
        device_cards.append(
            {
                "id": device["id"],
                "name": device["name"],
                "last_seen": device["last_seen"],
                "online": is_online,
                "latest": latest,
            }
        )

    latest_any = dict(readings[0]) if readings else None
    return {
        "user": user,
        "summary": {
            "device_count": len(devices),
            "online_devices": online,
            "reading_count": counts["reading_count"],
            "latest_temperature": latest_any["temperature"] if latest_any else None,
            "latest_humidity": latest_any["humidity"] if latest_any else None,
            "listeners": bus.listener_count,
        },
        "devices": device_cards,
        "recent_readings": [dict(r) for r in readings],
    }


@router.get("/devices")
def list_devices(user: dict = Depends(current_user)):
    with connect() as conn:
        rows = conn.execute(
            """
            SELECT id, name, api_key, last_seen, created_at
            FROM devices WHERE user_id = ? ORDER BY id
            """,
            (user["id"],),
        ).fetchall()
    return [
        {**dict(row), "online": _is_online(row["last_seen"])}
        for row in rows
    ]


@router.post("/devices")
def create_device(body: DeviceBody, user: dict = Depends(current_user)):
    import secrets

    api_key = secrets.token_urlsafe(24)
    now = utcnow()
    with connect() as conn:
        cur = conn.execute(
            """
            INSERT INTO devices (user_id, name, api_key, created_at)
            VALUES (?, ?, ?, ?)
            """,
            (user["id"], body.name.strip(), api_key, now),
        )
        conn.commit()
        device_id = cur.lastrowid
    event = {
        "type": "device_created",
        "user_id": user["id"],
        "device_id": device_id,
        "device_name": body.name.strip(),
        "created_at": now,
    }
    bus.publish(event)
    return {"id": device_id, "name": body.name.strip(), "api_key": api_key, "created_at": now}


@router.post("/devices/telemetry")
def ingest(body: TelemetryBody, device: dict = Depends(device_from_key)):
    """ESP32 (or any device) posts here with header X-Device-Key."""
    if body.temperature is None and body.humidity is None and not body.message:
        raise HTTPException(status_code=422, detail="Send temperature, humidity, or message")
    reading = _save_reading(device, body)
    return {"ok": True, "reading": reading}


@router.post("/stream/simulate")
def simulate(user: dict = Depends(current_user)):
    """Push one fake sensor event. Useful while learning Flow without hardware."""
    with connect() as conn:
        device = conn.execute(
            "SELECT id, user_id, name FROM devices WHERE user_id = ? ORDER BY id LIMIT 1",
            (user["id"],),
        ).fetchone()
    if device is None:
        raise HTTPException(status_code=404, detail="Create a device first")
    body = TelemetryBody(
        temperature=round(random.uniform(22.0, 32.0), 1),
        humidity=round(random.uniform(40.0, 75.0), 1),
        message=random.choice(
            ["desk sensor", "window open", "fan on", "all quiet", "motion nearby"]
        ),
    )
    reading = _save_reading(dict(device), body)
    return {"ok": True, "reading": reading}


@router.get("/stream/events")
async def events(user: dict = Depends(current_user)):
    """
    Server-Sent Events.

    Android: collect this response body as a Flow. Each `data:` line is one event.
    Events for other users are filtered out. A tick arrives every few seconds so
    the collector stays active without an ESP32.
    """
    queue = bus.subscribe()

    async def generate():
        try:
            yield sse(
                {
                    "type": "hello",
                    "user_id": user["id"],
                    "message": "stream open",
                    "created_at": utcnow(),
                }
            )
            while True:
                try:
                    event = await asyncio.wait_for(queue.get(), timeout=TICK_SECONDS)
                except asyncio.TimeoutError:
                    event = {
                        "type": "tick",
                        "user_id": user["id"],
                        "created_at": utcnow(),
                        "listeners": bus.listener_count,
                    }
                owner = event.get("user_id")
                if owner is not None and owner != user["id"]:
                    continue
                yield sse(event)
        finally:
            bus.unsubscribe(queue)

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


def _is_online(last_seen: Optional[str]) -> bool:
    if not last_seen:
        return False
    from datetime import datetime, timezone

    seen = datetime.strptime(last_seen, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)
    age = (datetime.now(timezone.utc) - seen).total_seconds()
    return age <= ONLINE_SECONDS
