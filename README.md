# Nandan

**A local-first personal AI operating system.**

Nandan is being built as a desktop-first assistant that can chat with a locally hosted model, retain user-controlled context, manage tasks and reminders, and later orchestrate approved workflows across the user's devices.

## Current milestone: local reminders

- React + TypeScript + Vite user interface
- FastAPI backend with local SQLite memory, task and reminder APIs
- Ollama local model integration (no hosted AI API required)
- Relevant saved memories are selected locally by keyword overlap and added to chat context (up to four matches)
- Reminder scheduler runs in the local backend process and promotes scheduled reminders to due status
- Browser notifications are optional and require the user's permission
- CI validates backend syntax, backend tests and frontend production build

This is an early development scaffold, not a production-ready assistant. Reminder delivery currently requires the local backend to remain running; browser notifications additionally require the UI/browser runtime and permission. OS-level delivery after the browser is closed, device pairing and remote notifications are future work.

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

Run backend tests from the backend directory with `pytest -q`.

## Local memory, tasks and reminders

SQLite database: `backend/data/nandan.db`.

- `GET/POST /api/memories` — list or save an explicitly entered memory
- `DELETE /api/memories/{id}` — delete a memory
- `GET/POST /api/tasks` — list or create tasks
- `PATCH /api/tasks/{id}` — update task details or completion
- `DELETE /api/tasks/{id}` — delete a task
- `GET/POST /api/reminders` — list or schedule a reminder
- `GET /api/reminders/due` — retrieve due reminders
- `POST /api/reminders/{id}/acknowledge` — mark a due reminder as acknowledged
- `DELETE /api/reminders/{id}` — delete a reminder

Reminder timestamps must include a timezone. The UI converts the device's local date/time to UTC before sending it to the backend. The backend scheduler checks every five seconds by default. Change `NANDAN_REMINDER_POLL_SECONDS` only if you add that setting to the environment configuration; otherwise the default is five seconds.

## Run the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the local Vite URL printed in the terminal (normally http://localhost:5173). In the Reminders workspace, grant notification permission to enable browser alerts. In-app due status is shown while the workspace is open.

## Configuration

Copy `backend/.env.example` to `backend/.env` and adjust values if needed. The default Ollama endpoint is `http://127.0.0.1:11434`.

## Safety and scope

- The backend binds to loopback by default; do not expose it to a public network.
- Reminder data stays in local SQLite; no cloud notification provider is used.
- Saved memory is user-controlled context, not an instruction override.
- The assistant does not execute shell commands, control other apps, send messages, publish content or spend money in this milestone.
- External actions will require explicit permission boundaries and audit logging before implementation.
- Never commit API keys, passwords, tokens or personal data.

See [docs/architecture.md](docs/architecture.md) for the staged architecture.
