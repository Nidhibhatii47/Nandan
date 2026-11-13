import { FormEvent, useEffect, useState } from "react";

type Message = { role: "user" | "assistant"; content: string };
type Task = { id: string; title: string; description: string; due_at: string | null; completed: boolean };
type Memory = { id: string; label: string; content: string; created_at: string };
type Health = { status: string; ollama_connected: boolean; model: string; storage?: string };
type Section = "assistant" | "tasks" | "memory";

const API_BASE = "http://127.0.0.1:8000";

async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(API_BASE + path, {
    ...init,
    headers: { "Content-Type": "application/json", ...init?.headers },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.detail ?? "Request failed (" + response.status + ")");
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export default function App() {
  const [section, setSection] = useState<Section>("assistant");
  const [messages, setMessages] = useState<Message[]>([
    { role: "assistant", content: "Welcome to Nandan. Your conversations use the local model when the desktop runtime is connected. Explicitly saved memories are retrieved when relevant. Memory and tasks are stored locally on that computer." },
  ]);
  const [input, setInput] = useState("");
  const [health, setHealth] = useState<Health | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [tasks, setTasks] = useState<Task[]>([]);
  const [memories, setMemories] = useState<Memory[]>([]);
  const [taskTitle, setTaskTitle] = useState("");
  const [taskDescription, setTaskDescription] = useState("");
  const [memoryLabel, setMemoryLabel] = useState("");
  const [memoryContent, setMemoryContent] = useState("");
  const [loadingData, setLoadingData] = useState(false);

  async function refreshTasks() { setTasks(await api<Task[]>("/api/tasks")); }
  async function refreshMemories() { setMemories(await api<Memory[]>("/api/memories")); }

  useEffect(() => {
    let active = true;
    const check = async () => {
      try {
        const data = await api<Health>("/api/health");
        if (active) setHealth(data);
      } catch {
        if (active) setHealth(null);
      }
    };
    void check();
    const timer = window.setInterval(check, 15000);
    return () => { active = false; window.clearInterval(timer); };
  }, []);

  useEffect(() => {
    let active = true;
    setLoadingData(true);
    const load = section === "tasks" ? refreshTasks : section === "memory" ? refreshMemories : async () => undefined;
    load()
      .catch((err: unknown) => { if (active) setError(err instanceof Error ? err.message : "Could not load local data."); })
      .finally(() => { if (active) setLoadingData(false); });
    return () => { active = false; };
  }, [section]);

  async function sendMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const message = input.trim();
    if (!message || busy) return;
    const history = [...messages, { role: "user" as const, content: message }].slice(-20);
    setMessages((current) => [...current, { role: "user", content: message }]);
    setInput("");
    setError("");
    setBusy(true);
    try {
      const data = await api<{ reply: string }>("/api/chat", {
        method: "POST",
        body: JSON.stringify({ message, history: history.slice(0, -1) }),
      });
      setMessages((current) => [...current, { role: "assistant", content: data.reply }]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to reach Nandan API.");
    } finally { setBusy(false); }
  }

  async function addTask(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!taskTitle.trim()) return;
    setError("");
    try {
      await api<Task>("/api/tasks", { method: "POST", body: JSON.stringify({ title: taskTitle.trim(), description: taskDescription.trim() }) });
      setTaskTitle("");
      setTaskDescription("");
      await refreshTasks();
    } catch (err) { setError(err instanceof Error ? err.message : "Could not save task."); }
  }

  async function toggleTask(task: Task) {
    try {
      await api<Task>("/api/tasks/" + task.id, { method: "PATCH", body: JSON.stringify({ completed: !task.completed }) });
      await refreshTasks();
    } catch (err) { setError(err instanceof Error ? err.message : "Could not update task."); }
  }

  async function removeTask(taskId: string) {
    try { await api<void>("/api/tasks/" + taskId, { method: "DELETE" }); await refreshTasks(); }
    catch (err) { setError(err instanceof Error ? err.message : "Could not delete task."); }
  }

  async function addMemory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!memoryLabel.trim() || !memoryContent.trim()) return;
    setError("");
    try {
      await api<Memory>("/api/memories", { method: "POST", body: JSON.stringify({ label: memoryLabel.trim(), content: memoryContent.trim() }) });
      setMemoryLabel("");
      setMemoryContent("");
      await refreshMemories();
    } catch (err) { setError(err instanceof Error ? err.message : "Could not save memory."); }
  }

  async function removeMemory(memoryId: string) {
    try { await api<void>("/api/memories/" + memoryId, { method: "DELETE" }); await refreshMemories(); }
    catch (err) { setError(err instanceof Error ? err.message : "Could not delete memory."); }
  }

  const heading = section === "assistant" ? "Assistant" : section === "tasks" ? "Tasks" : "Memory";

  return (
    <main className="shell">
      <aside className="sidebar">
        <div className="brand-mark">N</div>
        <div><p className="eyebrow">PERSONAL AI OS</p><h1>Nandan</h1></div>
        <div className="side-rule" />
        <p className="side-label">WORKSPACE</p>
        <button className={"nav-item " + (section === "assistant" ? "active" : "")} onClick={() => { setSection("assistant"); setError(""); }}>◉ &nbsp; Assistant</button>
        <button className={"nav-item " + (section === "tasks" ? "active" : "")} onClick={() => { setSection("tasks"); setError(""); }}>▦ &nbsp; Tasks <small>{tasks.length}</small></button>
        <button className={"nav-item " + (section === "memory" ? "active" : "")} onClick={() => { setSection("memory"); setError(""); }}>◈ &nbsp; Memory <small>{memories.length}</small></button>
        <div className="nav-item muted">✳ &nbsp; Creator studio <small>Planned</small></div>
        <div className="sidebar-bottom"><span className="privacy-dot" /><span>Local-first by design</span></div>
      </aside>

      <section className="main-panel">
        <header className="topbar">
          <div><p className="eyebrow">YOUR PRIVATE WORKSPACE</p><h2>{heading}</h2></div>
          <div className={"connection " + (health?.ollama_connected ? "online" : "")}><span className="status-dot" />{health?.ollama_connected ? "Local model · " + health.model : "Local runtime not connected"}</div>
        </header>

        {error && <div className="error-message" role="alert">{error}</div>}

        {section === "assistant" && <>
          <div className="memory-status" role="note">◈ &nbsp; Relevant saved memories are added to chat context automatically. Only memories you explicitly save are used.</div>
          <section className="conversation" aria-live="polite">
            {messages.map((item, index) => <article className={"message " + item.role} key={index}>
              <div className="avatar">{item.role === "assistant" ? "N" : "Y"}</div>
              <div className="message-body"><p className="message-author">{item.role === "assistant" ? "Nandan" : "You"}</p><p>{item.content}</p></div>
            </article>)}
            {busy && <p className="thinking">Nandan is thinking…</p>}
          </section>
          <form className="composer" onSubmit={sendMessage}>
            <label className="sr-only" htmlFor="prompt">Message Nandan</label>
            <textarea id="prompt" value={input} onChange={(event) => setInput(event.target.value)} onKeyDown={(event) => {
              if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); event.currentTarget.form?.requestSubmit(); }
            }} placeholder="Ask Nandan anything…" rows={2} maxLength={12000} />
            <div className="composer-footer"><span>Local model · Prompts stay on this computer</span><button type="submit" disabled={busy || !input.trim()}>{busy ? "Working…" : "Send ↑"}</button></div>
          </form>
        </>}

        {section === "tasks" && <section className="workspace-content">
          <p className="section-intro">Create and track tasks stored in Nandan's local database.</p>
          <form className="data-form" onSubmit={addTask}>
            <input value={taskTitle} onChange={(event) => setTaskTitle(event.target.value)} placeholder="What needs to get done?" maxLength={200} required />
            <input value={taskDescription} onChange={(event) => setTaskDescription(event.target.value)} placeholder="Description (optional)" maxLength={2000} />
            <button type="submit" disabled={!taskTitle.trim()}>Add task</button>
          </form>
          {loadingData ? <p className="empty-state">Loading tasks…</p> : tasks.length === 0 ? <p className="empty-state">No tasks yet. Add your first task above.</p> :
            <div className="item-list">{tasks.map((task) => <article className={"data-item " + (task.completed ? "done" : "")} key={task.id}>
              <label className="task-check"><input type="checkbox" checked={task.completed} onChange={() => void toggleTask(task)} /><span><strong>{task.title}</strong>{task.description && <small>{task.description}</small>}</span></label>
              <button className="delete-button" onClick={() => void removeTask(task.id)} aria-label={"Delete " + task.title}>Delete</button>
            </article>)}</div>}
        </section>}

        {section === "memory" && <section className="workspace-content">
          <p className="section-intro">Only save information you explicitly enter. You can review or delete every saved memory. Matching memories are retrieved for relevant chat requests.</p>
          <form className="data-form" onSubmit={addMemory}>
            <input value={memoryLabel} onChange={(event) => setMemoryLabel(event.target.value)} placeholder="Memory title" maxLength={100} required />
            <textarea value={memoryContent} onChange={(event) => setMemoryContent(event.target.value)} placeholder="What should Nandan remember?" rows={3} maxLength={4000} required />
            <button type="submit" disabled={!memoryLabel.trim() || !memoryContent.trim()}>Save memory</button>
          </form>
          {loadingData ? <p className="empty-state">Loading memories…</p> : memories.length === 0 ? <p className="empty-state">No saved memories. Add information you want to retain.</p> :
            <div className="item-list">{memories.map((memory) => <article className="data-item memory-item" key={memory.id}>
              <div><strong>{memory.label}</strong><p>{memory.content}</p><small>{new Date(memory.created_at).toLocaleString()}</small></div>
              <button className="delete-button" onClick={() => void removeMemory(memory.id)}>Delete</button>
            </article>)}</div>}
        </section>}

        <p className="footer-note">Milestone 3 · Local memory retrieval · External actions are not enabled</p>
      </section>
    </main>
  );
}
