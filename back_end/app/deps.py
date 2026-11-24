import json
from typing import Optional

import jwt
from fastapi import Depends, Header, HTTPException, Query
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.db import connect
from app.security import decode_token

bearer = HTTPBearer(auto_error=False)


def _user_from_token(token: str) -> dict:
    try:
        payload = decode_token(token)
        user_id = int(payload["sub"])
    except (jwt.PyJWTError, KeyError, ValueError):
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    with connect() as conn:
        row = conn.execute(
            "SELECT id, email, name, created_at FROM users WHERE id = ?",
            (user_id,),
        ).fetchone()
    if row is None:
        raise HTTPException(status_code=401, detail="User not found")
    return dict(row)


def current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(bearer),
    token: Optional[str] = Query(default=None),
) -> dict:
    """Accept Authorization: Bearer, or ?token= for browser EventSource."""
    raw = credentials.credentials if credentials else token
    if not raw:
        raise HTTPException(status_code=401, detail="Missing token")
    return _user_from_token(raw)


def device_from_key(x_device_key: str = Header(...)) -> dict:
    with connect() as conn:
        row = conn.execute(
            """
            SELECT d.id, d.user_id, d.name, d.api_key, d.last_seen, d.created_at,
                   u.email AS owner_email
            FROM devices d
            JOIN users u ON u.id = d.user_id
            WHERE d.api_key = ?
            """,
            (x_device_key,),
        ).fetchone()
    if row is None:
        raise HTTPException(status_code=401, detail="Unknown device key")
    return dict(row)


def sse(event: dict) -> str:
    return f"data: {json.dumps(event)}\n\n"
