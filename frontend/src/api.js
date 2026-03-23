const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export async function apiRequest(path, options = {}) {
  const token = localStorage.getItem('token');
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (!response.ok) {
    const payload = await response.json().catch(() => ({}));
    throw new Error(payload.error || `HTTP ${response.status}`);
  }
  return response.status === 204 ? null : response.json();
}

export function register(username, password) {
  return apiRequest('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  });
}

export function login(username, password) {
  return apiRequest('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  });
}

export function listDreams() {
  return apiRequest('/dreams');
}

export function createDream(title) {
  return apiRequest('/dreams', {
    method: 'POST',
    body: JSON.stringify({ title })
  });
}

export function getDream(id) {
  return apiRequest(`/dreams/${id}`);
}

export function sendDreamMessage(id, content) {
  return apiRequest(`/dreams/${id}/messages`, {
    method: 'POST',
    body: JSON.stringify({ content })
  });
}

export function completeDream(id) {
  return apiRequest(`/dreams/${id}/complete`, {
    method: 'POST'
  });
}
