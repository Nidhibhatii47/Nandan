from datetime import datetime, timedelta, timezone
import sqlite3

from app import main


def configure_database(tmp_path, monkeypatch):
    monkeypatch.setattr(main.settings, "database_path", str(tmp_path / "nandan-test.db"))
    main.initialize_database()


def insert_due_reminder(reminder_id="r1"):
    now = datetime.now(timezone.utc)
    with main.connect_db() as db:
        db.execute(
            "INSERT INTO reminders (id, title, note, scheduled_at, status) "
            "VALUES (?, ?, ?, ?, 'due')",
            (reminder_id, "Test reminder", "Test note", (now - timedelta(minutes=1)).isoformat()),
        )
    return now


def test_due_reminder_is_delivered_once_and_persisted(tmp_path, monkeypatch):
    configure_database(tmp_path, monkeypatch)
    now = insert_due_reminder()
    sent = []
    monkeypatch.setattr(main, "send_native_notification", lambda title, note: sent.append((title, note)) or True)

    assert main.deliver_due_notifications(now) == 1
    assert main.deliver_due_notifications(now + timedelta(seconds=5)) == 0
    assert sent == [("Test reminder", "Test note")]

    with main.connect_db() as db:
        reminder = db.execute("SELECT * FROM reminders WHERE id = 'r1'").fetchone()
    assert reminder["notified_at"] is not None
    assert reminder["notification_attempts"] == 1
    assert reminder["notification_error"] is None


def test_failed_native_notification_is_retried_after_backoff(tmp_path, monkeypatch):
    configure_database(tmp_path, monkeypatch)
    now = insert_due_reminder()
    monkeypatch.setattr(main.settings, "reminder_retry_seconds", 10)
    attempts = []
    def fail_once(title, note):
        attempts.append(title)
        return len(attempts) > 1
    monkeypatch.setattr(main, "send_native_notification", fail_once)

    assert main.deliver_due_notifications(now) == 0
    assert main.deliver_due_notifications(now + timedelta(seconds=5)) == 0
    assert main.deliver_due_notifications(now + timedelta(seconds=11)) == 1
    assert attempts == ["Test reminder", "Test reminder"]

    with main.connect_db() as db:
        reminder = db.execute("SELECT * FROM reminders WHERE id = 'r1'").fetchone()
    assert reminder["notification_attempts"] == 2
    assert reminder["notified_at"] is not None


def test_existing_reminder_database_gets_notification_columns(tmp_path, monkeypatch):
    database = tmp_path / "legacy.db"
    connection = sqlite3.connect(database)
    connection.execute("""
        CREATE TABLE reminders (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            note TEXT NOT NULL DEFAULT '',
            scheduled_at TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'scheduled',
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            acknowledged_at TEXT
        )
    """)
    connection.commit()
    connection.close()
    monkeypatch.setattr(main.settings, "database_path", str(database))

    main.initialize_database()

    with main.connect_db() as db:
        columns = {row["name"] for row in db.execute("PRAGMA table_info(reminders)")}
    assert {"notified_at", "last_notification_attempt_at", "notification_attempts", "notification_error"} <= columns
