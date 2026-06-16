import { state, AUTH_TOKEN_KEY, AUTH_USER_KEY } from './state.js';
import { apiRequest } from './api.js';
import { showToast, isTokenExpired } from './utils.js';

// salva token e dados do usuario no localStorage e atualiza o state
export function saveAuth(token, user) {
  localStorage.setItem(AUTH_TOKEN_KEY, token);
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
  state.isAuthenticated = true;
  state.user = user;
}

// remove dados de autenticacao do localStorage e limpa o state
export function clearAuth() {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(AUTH_USER_KEY);
  state.isAuthenticated = false;
  state.user = null;
}

// verifica se o usuario esta autenticado ao carregar a pagina
export function checkAuth() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  const userStr = localStorage.getItem(AUTH_USER_KEY);

  state.isAuthenticated = false;
  state.user = null;

  if (token && userStr) {
    // se o token expirou, faz logout automatico
    if (isTokenExpired(token)) {
      clearAuth();
      return;
    }
    try {
      state.isAuthenticated = true;
      state.user = JSON.parse(userStr);
    } catch (_) {
      clearAuth();
    }
  }
}

// verifica se esta autenticado, se nao redireciona para cadastro
export function ensureAuthenticated(navigateFn) {
  checkAuth();
  if (!state.isAuthenticated) {
    showToast("Faça seu cadastro para adicionar ao carrinho.", "error");
    navigateFn("register");
    return false;
  }
  return true;
}

// faz login enviando email e senha para a API
export async function login(email, senha) {
  const data = await apiRequest("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, senha }),
  });
  saveAuth(data.token, { email: data.email, role: data.role });
  await ensureUserId();
  return data;
}

// cria uma nova conta de usuario na API
export async function register(userData) {
  return await apiRequest("/api/usuarios", {
    method: "POST",
    body: JSON.stringify(userData),
  });
}

// faz logout limpando dados e redirecionando para home
export function logout(navigateFn) {
  clearAuth();
  state.cart = [];
  navigateFn("home");
}

// busca o id do usuario logado na API (necessario para compras)
export async function ensureUserId() {
  if (!state.isAuthenticated || !state.user?.email) return null;
  if (state.user.id) return state.user.id;

  try {
    const data = await apiRequest("/api/usuarios?page=0&size=1000");
    const list = data.content || data || [];
    const usuario = list.find((u) => u.email === state.user.email);
    if (usuario) {
      state.user = { ...state.user, id: usuario.id };
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(state.user));
      return usuario.id;
    }
  } catch (err) {
    console.error("Erro ao carregar usuário logado:", err);
  }
  return null;
}
