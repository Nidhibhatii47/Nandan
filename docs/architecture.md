# Nandan architecture (staged)

## Product direction

Nandan is a local-first, desktop-first personal AI operating system. The Android device is intended to act as a companion/control surface; the desktop machine hosts the local runtime and model initially.

## Current foundation

```
React + TypeScript (Vite)
        |
        | HTTP on localhost
        v
FastAPI (127.0.0.1:8000)
        |                    |
        | Ollama API         | local SQLite
        v                    v
Local language model   Memories / tasks / reminders
                             ^
                             |
                  Local asyncio reminder worker
```

The browser UI and backend are separate processes. The backend must be running on the desktop for the UI to reach its local Ollama service. The reminder worker runs inside the backend process and promotes due reminders every few seconds. The UI polls the local API and can display browser notifications after explicit permission is granted.

## Implemented modules

1. **Conversation and memory** — explicit memory controls and local keyword-based retrieval.
2. **Tasks** — local task creation, update, completion and deletion.
3. **Reminders** — timezone-aware schedules, due-state promotion, acknowledgement and deletion; browser alerts are opt-in.

## Planned modules

4. **Agent runtime** — bounded tools, plans, approvals, execution logs and recovery.
5. **Creator studio** — research, scripts, assets, narration and publishing workflow; publishing requires approval.
6. **Work assistant** — job discovery, document preparation and follow-up tracking; submitting applications requires approval.
7. **Device bridge** — authenticated pairing between desktop host and Android client.

## Security principles

- Bind local services to loopback by default.
- Do not expose Ollama or FastAPI directly to the internet.
- Add authentication and device pairing before any LAN or remote access.
- Keep secrets in ignored environment files.
- Require explicit approval for sending, publishing, purchasing, deleting or other consequential actions.
- Log tool invocations and outcomes; provide a stop control.

## Delivery sequence

- M0: repository scaffold, health endpoint and local-model chat.
- M1: local persistence and automated tests.
- M2: memory retrieval and task manager.
- M3/M4: local reminder scheduler and browser notification surface.
- Next: permissioned tool registry and agent planning.
- Later: Android companion, secure pairing and controlled integrations.

Each milestone must be tested before being described as complete.
