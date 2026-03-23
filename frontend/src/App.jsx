import { useEffect, useMemo, useState } from 'react';
import {
  completeDream,
  createDream,
  getDream,
  listDreams,
  login,
  register,
  sendDreamMessage
} from './api';

function AuthForm({ onAuth }) {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  async function submit(e) {
    e.preventDefault();
    setError('');
    try {
      const response = isLogin ? await login(username, password) : await register(username, password);
      localStorage.setItem('token', response.token);
      localStorage.setItem('username', response.username);
      onAuth(response.username);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="auth-wrap">
      <h1>Psy By Agent</h1>
      <p>Интерпретация снов с локальным Qwen</p>
      <form className="card" onSubmit={submit}>
        <h2>{isLogin ? 'Вход' : 'Регистрация'}</h2>
        <input placeholder="Логин" value={username} onChange={(e) => setUsername(e.target.value)} />
        <input
          placeholder="Пароль"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {error && <div className="error">{error}</div>}
        <button type="submit">{isLogin ? 'Войти' : 'Зарегистрироваться'}</button>
        <button type="button" className="ghost" onClick={() => setIsLogin((v) => !v)}>
          {isLogin ? 'Нет аккаунта? Регистрация' : 'Уже есть аккаунт? Вход'}
        </button>
      </form>
    </div>
  );
}

function AppShell({ username, onLogout }) {
  const [dreams, setDreams] = useState([]);
  const [activeId, setActiveId] = useState(null);
  const [activeDream, setActiveDream] = useState(null);
  const [message, setMessage] = useState('');
  const [newDreamTitle, setNewDreamTitle] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function refreshDreams(selectFirst = false) {
    const data = await listDreams();
    setDreams(data);
    if (selectFirst && data.length > 0) {
      await openDream(data[0].id);
    }
  }

  async function openDream(id) {
    setActiveId(id);
    const dream = await getDream(id);
    setActiveDream(dream);
  }

  useEffect(() => {
    refreshDreams(true).catch((e) => setError(e.message));
  }, []);

  async function createNewDream() {
    const title = newDreamTitle.trim();
    if (!title) return;
    try {
      const created = await createDream(title);
      setNewDreamTitle('');
      await refreshDreams();
      await openDream(created.id);
    } catch (e) {
      setError(e.message);
    }
  }

  async function sendMessage() {
    if (!activeId || !message.trim()) return;
    setLoading(true);
    setError('');
    try {
      const updated = await sendDreamMessage(activeId, message.trim());
      setActiveDream(updated);
      setMessage('');
      await refreshDreams();
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function finishDream() {
    if (!activeId) return;
    try {
      const updated = await completeDream(activeId);
      setActiveDream(updated);
      await refreshDreams();
    } catch (e) {
      setError(e.message);
    }
  }

  const canSend = useMemo(() => activeDream && activeDream.status === 'ACTIVE', [activeDream]);

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-top">
          <strong>{username}</strong>
          <input
            value={newDreamTitle}
            onChange={(e) => setNewDreamTitle(e.target.value)}
            placeholder="Название нового сна"
          />
          <button onClick={createNewDream} disabled={!newDreamTitle.trim()}>
            Новый сон
          </button>
          <button className="ghost" onClick={onLogout}>
            Выйти
          </button>
        </div>
        <div className="dream-list">
          {dreams.map((dream) => (
            <button
              key={dream.id}
              className={`dream-item ${dream.id === activeId ? 'active' : ''}`}
              onClick={() => openDream(dream.id)}
            >
              <div>{dream.title}</div>
              <small>{dream.status}</small>
            </button>
          ))}
        </div>
      </aside>

      <main className="chat">
        {!activeDream ? (
          <div className="empty">Выбери сон слева или создай новый.</div>
        ) : (
          <>
            <div className="chat-header">
              <h2>{activeDream.title}</h2>
              <button onClick={finishDream} disabled={activeDream.status === 'COMPLETED'}>
                Завершить обсуждение
              </button>
            </div>
            <div className="messages">
              {(activeDream.messages || []).map((m) => (
                <div key={m.id} className={`msg ${m.sender === 'USER' ? 'me' : 'bot'}`}>
                  <strong>{m.sender === 'USER' ? 'Вы' : 'Ассистент'}:</strong> {m.content}
                </div>
              ))}
            </div>
            {activeDream.finalInterpretation && (
              <div className="final">
                <strong>Итоговая интерпретация:</strong>
                <p>{activeDream.finalInterpretation}</p>
              </div>
            )}
            <div className="composer">
              <textarea
                placeholder="Опишите сон или задайте вопрос..."
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                disabled={!canSend || loading}
              />
              <button onClick={sendMessage} disabled={!canSend || loading}>
                {loading ? 'Отправка...' : 'Отправить'}
              </button>
            </div>
          </>
        )}
        {error && <div className="error">{error}</div>}
      </main>
    </div>
  );
}

export default function App() {
  const [username, setUsername] = useState(localStorage.getItem('username') || '');

  function handleLogout() {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    setUsername('');
  }

  if (!username) {
    return <AuthForm onAuth={setUsername} />;
  }

  return <AppShell username={username} onLogout={handleLogout} />;
}
