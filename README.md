# Nandan

**A local-first personal AI operating system.**

Nandan is being built as a desktop-first assistant that can chat with a locally hosted model, retain user-controlled context, manage tasks, and later orchestrate approved workflows across the user's devices.

## Current milestone: foundation

- React + TypeScript + Vite user interface
- FastAPI backend with local SQLite memory and task APIs
- Ollama local model integration (no hosted AI API required)
- Localhost-only backend binding by default
- Clear extension points for memory, tasks, agents, and creator tools

This is an early development scaffold, not a production-ready assistant. Persistent memory, task storage, device pairing, permissions, and workflow execution are planned milestones and are not implemented yet.

## Prerequisites

- Python 3.11+
- Node.js 20+
- Ollama installed and running
- A model pulled in Ollama, for example `qwen2.5:3b`

## Run the backend

From the repository root:

```bash
cd backend
python -m venv .venv
# Windows: .venv\\Scripts\\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

Backend health: http://127.0.0.1:8000/api/health

## Local memory and tasks

The API stores user-entered memories and tasks in a local SQLite database at `backend/data/nandan.db` by default. The database is created automatically when the API starts.

- `GET/POST /api/memories` — list or save an explicitly entered memory
- `DELETE /api/memories/{id}` — delete a memory
- `GET/POST /api/tasks` — list or create tasks
- `PATCH /api/tasks/{id}` — update task details or completion
- `DELETE /api/tasks/{id}` — delete a task

The frontend includes Tasks and Memory workspaces. These records are not automatically inferred from chat yet; users choose what to save.

## Run the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the local Vite URL printed in the terminal (normally http://localhost:5173).

## Configuration

Copy `backend/.env.example` to `backend/.env` and adjust values if needed. The default Ollama endpoint is `http://127.0.0.1:11434`.

## Safety and scope

- The backend binds to loopback by default; do not expose it to a public network.
- The assistant does not execute shell commands, control apps, send messages, publish content, or spend money in this milestone.
- External actions will require explicit permission boundaries and audit logging before implementation.
- Never commit API keys, passwords, tokens, or personal data.

See [docs/architecture.md](docs/architecture.md) for the staged architecture.
