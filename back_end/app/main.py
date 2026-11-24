from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

from app.config import ROOT
from app.db import init_db
from app.routes import router

app = FastAPI(
    title="FlowLab",
    description="Small API for practicing Android coroutines and Flow: auth, dashboard, live stream, ESP32 ingest.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router, prefix="/api")
app.mount("/static", StaticFiles(directory=ROOT / "static"), name="static")


@app.on_event("startup")
def startup() -> None:
    init_db()


@app.get("/")
def home():
    return FileResponse(ROOT / "static" / "index.html")
