import { state } from '../state.js';
import { apiRequest } from '../api.js';
import { showToast } from '../utils.js';
import { Header } from '../components/header.js';
import { Footer } from '../components/footer.js';

// pagina de notificacoes do usuario
export function NotificationsPage() {
  if (!state.isAuthenticated) {
    return `
      ${Header()}
      <main>
        <section class="page-section">
          <div class="container">
            <div class="empty-state">
              <i class="fas fa-lock"></i>
              <h3>Acesso Restrito</h3>
              <p>Faca login para ver suas notificacoes</p>
              <button class="btn btn-primary" onclick="navigate('login')">Entrar</button>
            </div>
          </div>
        </section>
      </main>
      ${Footer()}
    `;
  }

  const notifications = state.notifications || [];

  return `
    ${Header()}
    <main>
      <section class="page-section">
        <div class="container">
          <div class="section-header">
            <h1 class="page-title" style="margin-bottom: 0;">Notificacoes</h1>
            ${notifications.some(n => !n.lida) ? `
              <button class="btn btn-outline" onclick="markAllNotificationsRead()">
                <i class="fas fa-check-double"></i> Marcar todas como lidas
              </button>
            ` : ''}
          </div>

          ${notifications.length > 0 ? `
            <div class="notifications-list">
              ${notifications.map(n => NotificationItem(n)).join('')}
            </div>
          ` : `
            <div class="empty-state">
              <i class="fas fa-bell-slash"></i>
              <h3>Nenhuma notificacao</h3>
              <p>Voce nao tem notificacoes no momento</p>
            </div>
          `}
        </div>
      </section>
    </main>
    ${Footer()}
  `;
}

// item de notificacao individual
function NotificationItem(notification) {
  const icon = getNotificationIcon(notification.tipo);
  const dataFormatada = notification.dataCriacao ? new Date(notification.dataCriacao).toLocaleString() : '';

  return `
    <div class="notification-item ${notification.lida ? '' : 'notification-unread'}" onclick="markNotificationRead(${notification.id})">
      <div class="notification-icon">
        <i class="fas ${icon}"></i>
      </div>
      <div class="notification-content">
        <p class="notification-message">${notification.mensagem || notification.titulo || 'Notificacao'}</p>
        <span class="notification-date">${dataFormatada}</span>
      </div>
      ${!notification.lida ? '<span class="notification-dot"></span>' : ''}
    </div>
  `;
}

// retorna icone baseado no tipo de notificacao
function getNotificationIcon(tipo) {
  const icons = {
    TROCA_AUTORIZADA: 'fa-exchange-alt',
    CARRINHO_EXPIRANDO: 'fa-clock',
    ESTOQUE_ALTERADO: 'fa-warehouse',
    COMPRA_APROVADA: 'fa-check-circle',
    COMPRA_REPROVADA: 'fa-times-circle',
    COMPRA_EM_TRANSITO: 'fa-truck',
    COMPRA_ENTREGUE: 'fa-box',
    TROCA_RECEBIDA: 'fa-undo',
    CUPOM_GERADO: 'fa-ticket-alt',
    GERAL: 'fa-bell',
  };
  return icons[tipo] || 'fa-bell';
}

// carrega notificacoes do usuario
export async function loadNotifications() {
  try {
    const data = await apiRequest("/api/notificacoes");
    state.notifications = Array.isArray(data) ? data : (data.content || []);
  } catch (err) {
    console.error("Erro ao carregar notificacoes:", err);
    state.notifications = [];
  }
}

// carrega contagem de nao lidas
export async function loadNotificationCount() {
  try {
    const data = await apiRequest("/api/notificacoes/nao-lidas/contagem");
    state.notificationCount = Number(data) || 0;
  } catch (err) {
    state.notificationCount = 0;
  }
}

// marca uma notificacao como lida
export async function markNotificationRead(id) {
  try {
    await apiRequest(`/api/notificacoes/${id}/lida`, { method: "PUT" });
    const notif = (state.notifications || []).find(n => n.id === id);
    if (notif) notif.lida = true;
    if (state.notificationCount > 0) state.notificationCount--;
    window.render();
  } catch (err) {
    console.error("Erro ao marcar notificacao como lida:", err);
  }
}

// marca todas como lidas
export async function markAllNotificationsRead() {
  try {
    const unread = (state.notifications || []).filter(n => !n.lida);
    for (const n of unread) {
      await apiRequest(`/api/notificacoes/${n.id}/lida`, { method: "PUT" });
      n.lida = true;
    }
    state.notificationCount = 0;
    showToast("Todas as notificacoes foram marcadas como lidas!");
    window.render();
  } catch (err) {
    showToast("Erro ao marcar notificacoes: " + err.message, "error");
  }
}
