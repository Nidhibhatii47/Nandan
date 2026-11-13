# Nandan

**A local-first personal AI operating system.**

Nandan is being built as a desktop-first assistant that can chat with a locally hosted model, retain user-controlled context, manage tasks and reminders, and later orchestrate approved workflows across the user's devices.

## Current milestone: local reminders

- React + TypeScript + Vite user interface
- FastAPI backend with local SQLite memory, task and reminder APIs
- Ollama local model integration (no hosted AI API required)
- Relevant saved memories are selected locally by keyword overlap and added to chat context (up to four matches)
- Reminder scheduler runs in the local backend process, recovers missed reminders after restart, and persists notification delivery state
- On Windows, the backend sends native Windows toast notifications even when the web interface is closed
- A Windows Task Scheduler installer can start the backend automatically at user sign-in and restart it after failures
- Native notification attempts are retried with a bounded interval; successful delivery is recorded to avoid duplicate toasts
- CI validates backend syntax, reminder recovery/delivery tests and frontend production build

This remains an early development scaffold, not a production-ready assistant. Windows native notifications require Windows 10/11, the Windows-only `winotify` dependency, and the background task to be installed. The computer must be powered on and the user must be signed in. Android native notifications, remote/device-paired reminders, and delivery while the computer is shut down are not included.

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

Reminder timestamps must include a timezone. The UI converts the device's local date/time to UTC before sending it to the backend. The backend scheduler checks every five seconds by default. Failed native notification attempts are retried after 60 seconds by default. Configure `REMINDER_POLL_SECONDS` and `REMINDER_RETRY_SECONDS` in `backend/.env` if needed.

## Run the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the local Vite URL printed in the terminal (normally http://localhost:5173). In-app due status is shown while the workspace is open. On Windows, the backend can send native toast notifications independently of the browser UI.

## Windows background reminders

After installing backend dependencies and confirming the backend starts successfully, open PowerShell in the repository root and run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install_windows_startup.ps1
```

This registers and starts a per-user Task Scheduler task named `Nandan Reminder Engine`. It starts the local API at sign-in and writes logs to `backend/logs/reminder-engine.log`. Verify the API at http://127.0.0.1:8000/api/health; the response should report `reminder_scheduler_running: true` and `native_notifications: "windows-toast"`.

To remove automatic startup:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\uninstall_windows_startup.ps1
```

The task runs under the signed-in Windows user. Windows notification settings must allow notifications for the Python/Windows app. The machine must be on and the user signed in for scheduled reminders to fire.

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
