// exibe uma notificacao toast temporaria na tela
export function showToast(message, type = "success") {
  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  document.body.appendChild(toast);

  // mostra o toast com animacao
  setTimeout(() => {
    toast.classList.add("show");
  }, 100);

  // remove o toast apos 3 segundos
  setTimeout(() => {
    toast.classList.remove("show");
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}

// decodifica o payload de um token JWT (base64)
export function parseJwt(token) {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(
      normalized.length + ((4 - (normalized.length % 4)) % 4),
      "="
    );
    return JSON.parse(atob(padded));
  } catch (_) {
    return null;
  }
}

// verifica se o token JWT esta expirado
export function isTokenExpired(token) {
  const payload = parseJwt(token);
  if (!payload || typeof payload.exp !== "number") return true;
  return Date.now() >= payload.exp * 1000;
}
