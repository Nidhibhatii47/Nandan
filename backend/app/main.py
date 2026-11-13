from __future__ import annotations

import os
from typing import Literal

import httpx
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    ollama_base_url: str = "http://127.0.0.1:11434"
    ollama_model: str = "qwen2.5:3b"
    ollama_timeout_seconds: float = 120.0


settings = Settings()

app = FastAPI(
    title="Nandan API",
    version="0.1.0",
    description="Local-first personal AI operating system API.",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://127.0.0.1:5173",
    ],
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type"],
)


class ChatMessage(BaseModel):
    role: Literal["user", "assistant"] = "user"
    content: str = Field(min_length=1, max_length=12000)


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=12000)
    history: list[ChatMessage] = Field(default_factory=list, max_length=30)


class ChatResponse(BaseModel):
    reply: str
    model: str


@app.get("/api/health")
async def health() -> dict[str, str | bool]:
    """Report API status and whether the configured local model service responds."""
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
    }


@app.post("/api/chat", response_model=ChatResponse)
async def chat(payload: ChatRequest) -> ChatResponse:
    """Send a conversation turn to the locally hosted Ollama model."""
    messages = [
        {"role": item.role, "content": item.content}
        for item in payload.history
    ]
    messages.append({"role": "user", "content": payload.message})

    try:
        async with httpx.AsyncClient(timeout=settings.ollama_timeout_seconds) as client:
            response = await client.post(
                f"{settings.ollama_base_url.rstrip('/')}/api/chat",
                json={
                    "model": settings.ollama_model,
                    "messages": messages,
                    "stream": False,
                },
            )
            response.raise_for_status()
            data = response.json()
    except httpx.ConnectError as exc:
        raise HTTPException(
            status_code=503,
            detail="Ollama is not reachable. Start Ollama on this computer and try again.",
        ) from exc
    except httpx.TimeoutException as exc:
        raise HTTPException(
            status_code=504,
            detail="The local model took too long to respond. Try a smaller model or retry.",
        ) from exc
    except httpx.HTTPStatusError as exc:
        raise HTTPException(
            status_code=502,
            detail=f"Ollama returned HTTP {exc.response.status_code}. Check the model name and Ollama logs.",
        ) from exc
    except (ValueError, KeyError) as exc:
        raise HTTPException(
            status_code=502,
            detail="Ollama returned an unexpected response.",
        ) from exc

    reply = data.get("message", {}).get("content", "").strip()
    if not reply:
        raise HTTPException(status_code=502, detail="The local model returned an empty response.")

    return ChatResponse(reply=reply, model=settings.ollama_model)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=os.getenv("NANDAN_HOST", "127.0.0.1"),
        port=int(os.getenv("NANDAN_PORT", "8000")),
        reload=True,
    )
