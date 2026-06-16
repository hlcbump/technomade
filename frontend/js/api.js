import { API_BASE, AUTH_TOKEN_KEY } from './state.js';

// funcao generica para fazer requisicoes HTTP ao backend
export async function apiRequest(url, options = {}) {
  // pega o token JWT do localStorage para autenticacao
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  const headers = {
    "Content-Type": "application/json",
    ...(token && { Authorization: `Bearer ${token}` }), // adiciona token se existir
    ...options.headers,
  };

  // faz a requisicao fetch para a API
  const res = await fetch(`${API_BASE}${url}`, {
    ...options,
    headers,
  });

  // tenta converter a resposta para JSON
  let data = null;
  const text = await res.text();
  if (text) {
    try {
      data = JSON.parse(text);
    } catch (_) {
      data = text;
    }
  }

  // lanca erro se a resposta nao foi ok (status 4xx ou 5xx)
  if (!res.ok) {
    throw new Error(typeof data === "string" ? data : JSON.stringify(data));
  }

  return data;
}
