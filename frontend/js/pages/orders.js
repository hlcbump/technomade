import { state } from '../state.js';
import { apiRequest } from '../api.js';
import { showToast } from '../utils.js';
import { Header } from '../components/header.js';
import { Footer } from '../components/footer.js';

// mapeamento de status para labels e cores
const STATUS_MAP = {
  EM_PROCESSAMENTO: { label: 'Em Processamento', class: 'status-processing' },
  APROVADA: { label: 'Aprovada', class: 'status-approved' },
  REPROVADA: { label: 'Reprovada', class: 'status-rejected' },
  EM_TRANSITO: { label: 'Em Transito', class: 'status-transit' },
  ENTREGUE: { label: 'Entregue', class: 'status-delivered' },
  EM_TROCA: { label: 'Em Troca', class: 'status-exchange' },
  TROCA_AUTORIZADA: { label: 'Troca Autorizada', class: 'status-exchange-auth' },
  TROCA_RECUSADA: { label: 'Troca Recusada', class: 'status-rejected' },
  TROCADA: { label: 'Trocada', class: 'status-exchanged' },
};

function getStatusInfo(status) {
  return STATUS_MAP[status] || { label: status, class: 'status-default' };
}

// pagina "Meus Pedidos" - RF0025
export function OrdersPage() {
  if (!state.isAuthenticated) {
    return `
      ${Header()}
      <main>
        <section class="page-section">
          <div class="container">
            <div class="empty-state">
              <i class="fas fa-lock"></i>
              <h3>Acesso Restrito</h3>
              <p>Faca login para ver seus pedidos</p>
              <button class="btn btn-primary" onclick="navigate('login')">Entrar</button>
            </div>
          </div>
        </section>
      </main>
      ${Footer()}
    `;
  }

  const orders = (state.orders || []).slice().sort((a, b) => b.id - a.id);

  return `
    ${Header()}
    <main>
      <section class="page-section">
        <div class="container">
          <h1 class="page-title">Meus Pedidos</h1>

          ${orders.length > 0 ? `
            <div class="orders-list">
              ${orders.map(order => OrderCard(order)).join('')}
            </div>
          ` : `
            <div class="empty-state">
              <i class="fas fa-box-open"></i>
              <h3>Nenhum pedido encontrado</h3>
              <p>Voce ainda nao fez nenhuma compra</p>
              <button class="btn btn-primary" onclick="navigate('products')">Ver Produtos</button>
            </div>
          `}
        </div>
      </section>
    </main>
    ${Footer()}
  `;
}

// card de cada pedido com detalhes
function OrderCard(order) {
  const statusInfo = getStatusInfo(order.statusCompra);
  const dataCompra = order.dataCompra ? new Date(order.dataCompra).toLocaleDateString() : '-';
  const dataEntrega = order.dataEntregaPrevista
    ? new Date(order.dataEntregaPrevista + 'T00:00:00').toLocaleDateString('pt-BR')
    : '-';
  const itens = order.itens || order.itensCompra || [];
  const total = order.valorTotal != null ? Number(order.valorTotal).toFixed(2) : '0.00';

  return `
    <div class="order-card">
      <div class="order-header">
        <div class="order-info">
          <span class="order-id">Pedido #${order.id}</span>
          <span class="order-date">${dataCompra}</span>
          <span class="order-delivery-date">Entrega prevista: ${dataEntrega}</span>
        </div>
        <div class="order-status-actions">
          <span class="badge-status ${statusInfo.class}">${statusInfo.label}</span>
          <button class="btn btn-outline btn-sm" onclick="toggleOrderDetails(${order.id})">
            <i class="fas fa-chevron-down" id="order-icon-${order.id}"></i> Detalhes
          </button>
        </div>
      </div>

      <div class="order-summary-row">
        <span>${itens.length} ${itens.length === 1 ? 'item' : 'itens'}</span>
        <span class="order-total">R$ ${total}</span>
      </div>

      <div class="order-details" id="order-details-${order.id}" style="display: none;">
        <div class="divider"></div>

        <h4>Itens do Pedido</h4>
        <div class="order-items">
          ${itens.map(item => `
            <div class="order-item">
              <span class="order-item-name">${item.produto ? item.produto.nome : (item.nomeProduto || 'Produto')}</span>
              <span class="order-item-qty">Qtd: ${item.quantidade}</span>
              <span class="order-item-price">R$ ${item.precoUnitario != null ? Number(item.precoUnitario).toFixed(2) : '-'}</span>
            </div>
          `).join('')}
        </div>

        ${order.enderecoEntrega ? `
          <div class="order-address">
            <h4>Endereco de Entrega</h4>
            <p>${order.enderecoEntrega.logradouro || ''}, ${order.enderecoEntrega.numero || ''} - ${order.enderecoEntrega.cidade || ''}/${order.enderecoEntrega.estado || ''}</p>
          </div>
        ` : ''}

        ${order.frete != null ? `
          <div class="summary-row">
            <span>Frete:</span>
            <span>R$ ${Number(order.frete).toFixed(2)}</span>
          </div>
        ` : ''}

        ${order.statusCompra === 'ENTREGUE' ? `
          <div class="order-actions">
            <button class="btn btn-outline" onclick="showExchangeForm(${order.id})">
              <i class="fas fa-exchange-alt"></i> Solicitar Troca
            </button>
          </div>
          <div id="exchange-form-${order.id}"></div>
        ` : ''}
      </div>
    </div>
  `;
}

// alterna visibilidade dos detalhes do pedido
export function toggleOrderDetails(orderId) {
  const details = document.getElementById(`order-details-${orderId}`);
  const icon = document.getElementById(`order-icon-${orderId}`);
  if (details.style.display === 'none') {
    details.style.display = 'block';
    icon.classList.replace('fa-chevron-down', 'fa-chevron-up');
  } else {
    details.style.display = 'none';
    icon.classList.replace('fa-chevron-up', 'fa-chevron-down');
  }
}

// exibe formulario para solicitar troca - RF0040
export function showExchangeForm(orderId) {
  const order = (state.orders || []).find(o => o.id === orderId);
  if (!order) return;

  const itens = order.itens || order.itensCompra || [];
  const container = document.getElementById(`exchange-form-${orderId}`);

  container.innerHTML = `
    <div class="admin-form" style="margin-top: 1rem;">
      <h4>Solicitar Troca</h4>
      <p class="text-muted" style="margin-bottom: 1rem;">Selecione os itens e a quantidade que deseja trocar:</p>
      <form onsubmit="handleRequestExchange(event, ${orderId})">
        ${itens.map((item, idx) => `
          <div class="exchange-item" style="display: flex; align-items: center; gap: 1rem; padding: 0.5rem 0;">
            <input type="checkbox" name="item_${idx}" value="${item.id || idx}" />
            <span style="flex: 1;">${item.produto ? item.produto.nome : (item.nomeProduto || 'Produto')}</span>
            <label style="font-size: 0.875rem;">Qtd:</label>
            <input type="number" name="qty_${idx}" min="1" max="${item.quantidade}" value="1"
                   style="width: 60px; padding: 0.25rem; border: 2px solid var(--gray-light); border-radius: var(--radius-sm);" />
          </div>
        `).join('')}
        <div class="form-group" style="margin-top: 1rem;">
          <label>Motivo da Troca *</label>
          <textarea name="motivo" rows="3" required style="width: 100%; padding: 0.75rem; border: 2px solid var(--gray-light); border-radius: var(--radius-sm);"></textarea>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">Solicitar Troca</button>
          <button type="button" class="btn btn-outline" onclick="hideExchangeForm(${orderId})">Cancelar</button>
        </div>
      </form>
    </div>
  `;
}

export function hideExchangeForm(orderId) {
  document.getElementById(`exchange-form-${orderId}`).innerHTML = "";
}

// handler para solicitar troca - RF0040
export async function handleRequestExchange(e, orderId) {
  e.preventDefault();
  const formData = new FormData(e.target);
  const data = Object.fromEntries(formData);
  const order = (state.orders || []).find(o => o.id === orderId);
  if (!order) return;

  const itens = order.itens || order.itensCompra || [];
  const itensTroca = [];

  itens.forEach((item, idx) => {
    if (data[`item_${idx}`]) {
      itensTroca.push({
        produtoId: item.produto ? item.produto.id : item.produtoId,
        quantidade: Number(data[`qty_${idx}`] || 1),
      });
    }
  });

  if (itensTroca.length === 0) {
    showToast("Selecione pelo menos um item para troca.", "error");
    return;
  }

  try {
    await apiRequest("/api/trocas", {
      method: "POST",
      body: JSON.stringify({
        compraId: orderId,
        motivo: data.motivo,
        itens: itensTroca,
      }),
    });
    showToast("Troca solicitada com sucesso!");
    await loadOrders();
    window.render();
  } catch (err) {
    showToast("Erro ao solicitar troca: " + err.message, "error");
  }
}

// carrega pedidos do usuario logado
export async function loadOrders() {
  if (!state.user?.id) return;
  try {
    const data = await apiRequest(`/api/compras?usuarioId=${state.user.id}`);
    state.orders = Array.isArray(data) ? data : (data.content || []);
  } catch (err) {
    console.error("Erro ao carregar pedidos:", err);
    state.orders = [];
  }
}
