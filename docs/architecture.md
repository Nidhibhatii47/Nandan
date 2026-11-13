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
        |
        | Ollama API (127.0.0.1:11434)
        v
Local language model
```

The browser UI and backend are separate processes. The backend must be running on the desktop for the UI to reach its local Ollama service.

## Planned modules

1. **Conversation and memory** — user-owned conversation history, explicit memory controls, retrieval, export and deletion.
2. **Tasks and reminders** — local task store, schedules, notifications and completion state.
3. **Agent runtime** — bounded tools, plans, approvals, execution logs and recovery.
4. **Creator studio** — research, scripts, assets, narration and publishing workflow; publishing requires approval.
5. **Work assistant** — job discovery, document preparation and follow-up tracking; submitting applications requires approval.
6. **Device bridge** — authenticated pairing between desktop host and Android client.

## Security principles

- Bind local services to loopback by default.
- Do not expose Ollama or FastAPI directly to the internet.
- Add authentication and device pairing before any LAN or remote access.
- Keep secrets in ignored environment files.
- Require explicit approval for sending, publishing, purchasing, deleting or other consequential actions.
- Log tool invocations and outcomes; provide a stop control.

## Delivery sequence

- M0: repository scaffold, health endpoint, local-model chat (current).
- M1: local persistence, conversation history, settings and automated tests.
- M2: task manager and reminders.
- M3: permissioned tool registry and agent planning.
- M4: Android companion and secure device pairing.
- M5: creator/work workflows and controlled integrations.

Each milestone must be tested before being described as complete.
