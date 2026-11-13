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

    listed = client.get("/api/memories")
    assert listed.status_code == 200
    assert any(item["id"] == memory["id"] for item in listed.json())

    deleted = client.delete("/api/memories/" + memory["id"])
    assert deleted.status_code == 204
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

    listed = client.get("/api/tasks")
    assert listed.status_code == 200
    assert listed.json()[0]["id"] == task["id"]

    deleted = client.delete("/api/tasks/" + task["id"])
    assert deleted.status_code == 204
    assert client.get("/api/tasks").json() == []


def test_invalid_memory_is_rejected(tmp_path, monkeypatch):
    client = setup_client(tmp_path, monkeypatch)
    response = client.post("/api/memories", json={"label": "", "content": ""})
    assert response.status_code == 422
