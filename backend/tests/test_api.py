from datetime import datetime, timedelta, timezone

from fastapi.testclient import TestClient

from app import main


def setup_client(tmp_path, monkeypatch):
    monkeypatch.setattr(main.settings, "database_path", str(tmp_path / "nandan-test.db"))
    main.initialize_database()
    return TestClient(main.app)


def test_health_reports_local_storage(tmp_path, monkeypatch):
    client = setup_client(tmp_path, monkeypatch)
    response = client.get("/api/health")
    assert response.status_code == 200
    assert response.json()["storage"] == "local-sqlite"


def test_memory_create_list_delete(tmp_path, monkeypatch):
    client = setup_client(tmp_path, monkeypatch)
    created = client.post("/api/memories", json={
        "label": "Preferred language",
        "content": "Use concise English for technical instructions.",
    })
    assert created.status_code == 201
    memory = created.json()
    assert memory["label"] == "Preferred language"
    assert any(item["id"] == memory["id"] for item in client.get("/api/memories").json())
    assert client.delete("/api/memories/" + memory["id"]).status_code == 204
    assert client.get("/api/memories").json() == []


def test_task_create_complete_and_delete(tmp_path, monkeypatch):
    client = setup_client(tmp_path, monkeypatch)
    created = client.post("/api/tasks", json={
        "title": "Build Nandan memory engine",
        "description": "Add local persistence and API tests.",
    })
    assert created.status_code == 201
    task = created.json()
    assert task["completed"] is False
    updated = client.patch("/api/tasks/" + task["id"], json={"completed": True})
    assert updated.status_code == 200
    assert updated.json()["completed"] is True
    assert client.get("/api/tasks").json()[0]["id"] == task["id"]
    assert client.delete("/api/tasks/" + task["id"]).status_code == 204
    assert client.get("/api/tasks").json() == []


def test_invalid_memory_is_rejected(tmp_path, monkeypatch):
    client = setup_client(tmp_path, monkeypatch)
    response = client.post("/api/memories", json={"label": "", "content": ""})
    assert response.status_code == 422


def test_memory_retrieval_matches_relevant_and_excludes_unrelated(tmp_path, monkeypatch):
    setup_client(tmp_path, monkeypatch)
    with main.connect_db() as db:
        db.execute(
            "INSERT INTO memories (id, label, content) VALUES (?, ?, ?)",
            ("m1", "Work preference", "Use concise English for technical instructions."),
        )
        db.execute(
            "INSERT INTO memories (id, label, content) VALUES (?, ?, ?)",
            ("m2", "Food preference", "Vegetarian meals; avoid meat."),
        )
    selected = main.retrieve_relevant_memories("Help me write technical instructions in English")
    assert [item["id"] for item in selected] == ["m1"]


def test_chat_prompt_includes_only_relevant_saved_memory(tmp_path, monkeypatch):
    setup_client(tmp_path, monkeypatch)
    with main.connect_db() as db:
        db.execute(
            "INSERT INTO memories (id, label, content) VALUES (?, ?, ?)",
            ("m1", "Writing preference", "Use concise English for technical instructions."),
        )
        db.execute(
            "INSERT INTO memories (id, label, content) VALUES (?, ?, ?)",
            ("m2", "Unrelated", "Vegetarian meal planning."),
        )
    request = main.ChatRequest(message="Explain technical instructions in English")
    messages = main.build_chat_messages(request)
    assert messages[0]["role"] == "system"
    assert "Use concise English" in messages[0]["content"]
    assert "Vegetarian meal planning" not in messages[0]["content"]
    assert messages[-1] == {"role": "user", "content": request.message}


def test_reminder_is_scheduled_then_promoted_and_acknowledged(tmp_path, monkeypatch):
    client = setup_client(tmp_path, monkeypatch)
    future = datetime.now(timezone.utc) + timedelta(minutes=10)
    created = client.post("/api/reminders", json={
        "title": "Review Nandan",
        "note": "Check the reminder workflow",
        "scheduled_at": future.isoformat(),
    })
    assert created.status_code == 201
    reminder = created.json()
    assert reminder["status"] == "scheduled"
    assert client.get("/api/reminders/due").json() == []

    promoted = main.promote_due_reminders(future + timedelta(seconds=1))
    assert promoted == 1
    due = client.get("/api/reminders/due").json()
    assert len(due) == 1
    assert due[0]["id"] == reminder["id"]
    acknowledged = client.post(
        "/api/reminders/" + reminder["id"] + "/acknowledge"
    )
    assert acknowledged.status_code == 200
    assert acknowledged.json()["status"] == "acknowledged"
    assert client.get("/api/reminders/due").json() == []


def test_reminder_requires_timezone_aware_datetime(tmp_path, monkeypatch):
    client = setup_client(tmp_path, monkeypatch)
    response = client.post("/api/reminders", json={
        "title": "Timezone test",
        "scheduled_at": "2026-11-13T10:00:00",
    })
    assert response.status_code == 422


def test_reminder_can_be_deleted(tmp_path, monkeypatch):
    client = setup_client(tmp_path, monkeypatch)
    created = client.post("/api/reminders", json={
        "title": "Delete me",
        "scheduled_at": (datetime.now(timezone.utc) + timedelta(hours=1)).isoformat(),
    })
    reminder_id = created.json()["id"]
    assert client.delete("/api/reminders/" + reminder_id).status_code == 204
    assert client.delete("/api/reminders/" + reminder_id).status_code == 404
