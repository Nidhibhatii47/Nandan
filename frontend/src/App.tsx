import { FormEvent, useEffect, useState } from "react";

type Message = { role: "user" | "assistant"; content: string };
type Health = {
  status: string;
  ollama_connected: boolean;
  model: string;
};

const API_BASE = "http://127.0.0.1:8000";

export default function App() {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: "assistant",
      content:
        "Nandan is online as an interface. Start the local API and Ollama on your desktop to enable private, local-model conversations.",
    },
  ]);
  const [input, setInput] = useState("");
  const [health, setHealth] = useState<Health | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    const check = async () => {
      try {
        const response = await fetch(`${API_BASE}/api/health`);
        if (!response.ok) throw new Error("API unavailable");
        const data: Health = await response.json();
        if (active) setHealth(data);
      } catch {
        if (active) setHealth(null);
      }
    };
    void check();
    const timer = window.setInterval(check, 15000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  async function sendMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const message = input.trim();
    if (!message || busy) return;

    const prior = messages.filter((item) => item.role !== "assistant" || messages.indexOf(item) > 0);
    const nextMessages: Message[] = [...prior, { role: "user", content: message }];
    setMessages((current) => [...current, { role: "user", content: message }]);
    setInput("");
    setError("");
    setBusy(true);

    try {
      const response = await fetch(`${API_BASE}/api/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message,
          history: nextMessages.slice(-20),
        }),
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.detail ?? "The local assistant could not respond.");
      }
      setMessages((current) => [
        ...current,
        { role: "assistant", content: data.reply as string },
      ]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to reach Nandan API.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="shell">
      <aside className="sidebar">
        <div className="brand-mark">N</div>
        <div>
          <p className="eyebrow">PERSONAL AI OS</p>
          <h1>Nandan</h1>
        </div>
        <div className="side-rule" />
        <p className="side-label">WORKSPACE</p>
        <div className="nav-item active"><span>◉</span> Assistant</div>
        <div className="nav-item muted"><span>▦</span> Tasks <small>Planned</small></div>
        <div className="nav-item muted"><span>◈</span> Memory <small>Planned</small></div>
        <div className="nav-item muted"><span>✳</span> Creator studio <small>Planned</small></div>
        <div className="sidebar-bottom">
          <span className="privacy-dot" />
          <span>Local-first by design</span>
        </div>
      </aside>

      <section className="main-panel">
        <header className="topbar">
          <div>
            <p className="eyebrow">YOUR PRIVATE WORKSPACE</p>
            <h2>Assistant</h2>
          </div>
          <div className={`connection ${health?.ollama_connected ? "online" : ""}`}>
            <span className="status-dot" />
            {health?.ollama_connected ? `Local model · ${health.model}` : "Local runtime not connected"}
          </div>
        </header>

        <section className="conversation" aria-live="polite">
          {messages.map((item, index) => (
            <article className={`message ${item.role}`} key={index}>
              <div className="avatar">{item.role === "assistant" ? "N" : "Y"}</div>
              <div className="message-body">
                <p className="message-author">{item.role === "assistant" ? "Nandan" : "You"}</p>
                <p>{item.content}</p>
              </div>
            </article>
          ))}
          {busy && <p className="thinking">Nandan is thinking…</p>}
          {error && <div className="error-message">{error}</div>}
        </section>

        <form className="composer" onSubmit={sendMessage}>
          <label className="sr-only" htmlFor="prompt">Message Nandan</label>
          <textarea
            id="prompt"
            value={input}
            onChange={(event) => setInput(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                event.currentTarget.form?.requestSubmit();
              }
            }}
            placeholder="Ask Nandan anything…"
            rows={2}
            maxLength={12000}
          />
          <div className="composer-footer">
            <span>Local model · Your prompts stay on this computer</span>
            <button type="submit" disabled={busy || !input.trim()}> {busy ? "Working…" : "Send ↑"} </button>
          </div>
        </form>
        <p className="footer-note">Early foundation build · External actions are not enabled</p>
      </section>
    </main>
  );
}
