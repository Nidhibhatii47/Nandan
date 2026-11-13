from __future__ import annotations

import os
import sqlite3
from contextlib import contextmanager
from pathlib import Path
from typing import Iterator, Literal
from uuid import uuid4

import httpx
from fastapi import FastAPI, HTTPException, Response
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    ollama_base_url: str = "http://127.0.0.1:11434"
    ollama_model: str = "qwen2.5:3b"
    ollama_timeout_seconds: float = 120.0
    database_path: str = "data/nandan.db"


settings = Settings()
app = FastAPI(
    title="Nandan API",
    version="0.2.0",
    description="Local-first personal AI operating system API.",
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://127.0.0.1:5173"],
    allow_credentials=False,
    allow_methods=["GET", "POST", "PATCH", "DELETE"],
    allow_headers=["Content-Type"],
)


def database_file() -> Path:
    path = Path(settings.database_path).expanduser()
    if not path.is_absolute():
        path = Path(__file__).resolve().parents[1] / path
    path.parent.mkdir(parents=True, exist_ok=True)
    return path


@contextmanager
def connect_db() -> Iterator[sqlite3.Connection]:
    connection = sqlite3.connect(database_file(), timeout=10)
    connection.row_factory = sqlite3.Row
    try:
        connection.execute("PRAGMA foreign_keys = ON")
        yield connection
        connection.commit()
    finally:
        connection.close()


def initialize_database() -> None:
    with connect_db() as db:
        db.execute("""
            CREATE TABLE IF NOT EXISTS memories (
                id TEXT PRIMARY KEY,
                label TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """)
        db.execute("""
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                due_at TEXT,
                completed INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """)


@app.on_event("startup")
async def startup() -> None:
    initialize_database()


class ChatMessage(BaseModel):
    role: Literal["user", "assistant"] = "user"
    content: str = Field(min_length=1, max_length=12000)


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=12000)
    history: list[ChatMessage] = Field(default_factory=list, max_length=30)


class ChatResponse(BaseModel):
    reply: str
    model: str


class MemoryCreate(BaseModel):
    label: str = Field(min_length=1, max_length=100)
    content: str = Field(min_length=1, max_length=4000)


class TaskCreate(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(default="", max_length=2000)
    due_at: str | None = Field(default=None, max_length=80)


class TaskUpdate(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=200)
    description: str | None = Field(default=None, max_length=2000)
    due_at: str | None = Field(default=None, max_length=80)
    completed: bool | None = None


def row_dict(row: sqlite3.Row) -> dict:
    item = dict(row)
    if "completed" in item:
        item["completed"] = bool(item["completed"])
    return item


@app.get("/api/health")
async def health() -> dict[str, str | bool]:
    ollama_ok = False
    try:
        async with httpx.AsyncClient(timeout=3.0) as client:
            response = await client.get(f"{settings.ollama_base_url.rstrip('/')}/api/tags")
            ollama_ok = response.is_success
    except httpx.HTTPError:
        pass
    return {
        "status": "ok",
        "service": "nandan-api",
        "ollama_connected": ollama_ok,
        "model": settings.ollama_model,
        "storage": "local-sqlite",
    }


@app.post("/api/chat", response_model=ChatResponse)
async def chat(payload: ChatRequest) -> ChatResponse:
    messages = [{"role": item.role, "content": item.content} for item in payload.history]
    messages.append({"role": "user", "content": payload.message})
    try:
        async with httpx.AsyncClient(timeout=settings.ollama_timeout_seconds) as client:
            response = await client.post(
                f"{settings.ollama_base_url.rstrip('/')}/api/chat",
                json={"model": settings.ollama_model, "messages": messages, "stream": False},
            )
            response.raise_for_status()
            data = response.json()
    except httpx.ConnectError as exc:
        raise HTTPException(status_code=503, detail="Ollama is not reachable. Start Ollama on this computer and try again.") from exc
    except httpx.TimeoutException as exc:
        raise HTTPException(status_code=504, detail="The local model took too long to respond. Try again or use a smaller model.") from exc
    except httpx.HTTPStatusError as exc:
        raise HTTPException(status_code=502, detail=f"Ollama returned HTTP {exc.response.status_code}. Check the model name and Ollama logs.") from exc
    except (ValueError, KeyError) as exc:
        raise HTTPException(status_code=502, detail="Ollama returned an unexpected response.") from exc

    reply = data.get("message", {}).get("content", "").strip()
    if not reply:
        raise HTTPException(status_code=502, detail="The local model returned an empty response.")
    return ChatResponse(reply=reply, model=settings.ollama_model)


@app.get("/api/memories")
async def list_memories() -> list[dict]:
    with connect_db() as db:
        rows = db.execute("SELECT * FROM memories ORDER BY created_at DESC").fetchall()
    return [row_dict(row) for row in rows]


@app.post("/api/memories", status_code=201)
async def create_memory(payload: MemoryCreate) -> dict:
    memory_id = str(uuid4())
    with connect_db() as db:
        db.execute(
            "INSERT INTO memories (id, label, content) VALUES (?, ?, ?)",
            (memory_id, payload.label.strip(), payload.content.strip()),
        )
        row = db.execute("SELECT * FROM memories WHERE id = ?", (memory_id,)).fetchone()
    return row_dict(row)


@app.delete("/api/memories/{memory_id}", status_code=204)
async def delete_memory(memory_id: str) -> Response:
    with connect_db() as db:
        cursor = db.execute("DELETE FROM memories WHERE id = ?", (memory_id,))
    if cursor.rowcount == 0:
        raise HTTPException(status_code=404, detail="Memory not found.")
    return Response(status_code=204)


@app.get("/api/tasks")
async def list_tasks() -> list[dict]:
    with connect_db() as db:
        rows = db.execute(
            "SELECT * FROM tasks ORDER BY completed ASC, due_at IS NULL ASC, due_at ASC, created_at DESC"
        ).fetchall()
    return [row_dict(row) for row in rows]


@app.post("/api/tasks", status_code=201)
async def create_task(payload: TaskCreate) -> dict:
    task_id = str(uuid4())
    with connect_db() as db:
        db.execute(
            "INSERT INTO tasks (id, title, description, due_at) VALUES (?, ?, ?, ?)",
            (task_id, payload.title.strip(), payload.description.strip(), payload.due_at),
        )
        row = db.execute("SELECT * FROM tasks WHERE id = ?", (task_id,)).fetchone()
    return row_dict(row)


@app.patch("/api/tasks/{task_id}")
async def update_task(task_id: str, payload: TaskUpdate) -> dict:
    changes = payload.model_dump(exclude_unset=True)
    if not changes:
        raise HTTPException(status_code=400, detail="No task changes supplied.")
    if "title" in changes and changes["title"] is not None:
        changes["title"] = changes["title"].strip()
    if "description" in changes and changes["description"] is not None:
        changes["description"] = changes["description"].strip()
    columns = ", ".join(f"{column} = ?" for column in changes)
    values = [int(value) if key == "completed" and value is not None else value
              for key, value in changes.items()]
    with connect_db() as db:
        cursor = db.execute(f"UPDATE tasks SET {columns} WHERE id = ?", [*values, task_id])
        row = db.execute("SELECT * FROM tasks WHERE id = ?", (task_id,)).fetchone()
    if cursor.rowcount == 0 or row is None:
        raise HTTPException(status_code=404, detail="Task not found.")
    return row_dict(row)


@app.delete("/api/tasks/{task_id}", status_code=204)
async def delete_task(task_id: str) -> Response:
    with connect_db() as db:
        cursor = db.execute("DELETE FROM tasks WHERE id = ?", (task_id,))
    if cursor.rowcount == 0:
        raise HTTPException(status_code=404, detail="Task not found.")
    return Response(status_code=204)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=os.getenv("NANDAN_HOST", "127.0.0.1"),
        port=int(os.getenv("NANDAN_PORT", "8000")),
        reload=True,
    )
