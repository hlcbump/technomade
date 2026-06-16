import { state } from '../state.js';
import { Header } from '../components/header.js';
import { Footer } from '../components/footer.js';
import { getCartTotal } from '../cart.js';
import { apiRequest } from '../api.js';
import { showToast } from '../utils.js';

// componente de item individual do carrinho com controles de quantidade
export function CartItem(item) {
  const { product, quantity } = item;
  const subtotal = product.valorVenda * quantity;

  return `
    <div class="cart-item">
      <img src="${product.imagemUrl || 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22100%22 height=%22100%22%3E%3Crect fill=%22%23f3f4f6%22 width=%22100%22 height=%22100%22/%3E%3Ctext fill=%22%236b7280%22 font-family=%22Arial%22 font-size=%2214%22 x=%2250%25%22 y=%2250%25%22 text-anchor=%22middle%22 dy=%22.3em%22%3ESem Imagem%3C/text%3E%3C/svg%3E'}"
           alt="${product.nome}"
           onerror="if(!this.dataset.fallback){this.dataset.fallback='1';this.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22100%22 height=%22100%22%3E%3Crect fill=%22%23f3f4f6%22 width=%22100%22 height=%22100%22/%3E%3Ctext fill=%22%236b7280%22 font-family=%22Arial%22 font-size=%2214%22 x=%2250%25%22 y=%2250%25%22 text-anchor=%22middle%22 dy=%22.3em%22%3ESem Imagem%3C/text%3E%3C/svg%3E'}" />
      <div class="cart-item-info">
        <h4>${product.nome}</h4>
        <p class="text-muted">${product.descricao || ''}</p>
        <div class="cart-item-price">R$ ${product.valorVenda.toFixed(2)}</div>
      </div>
      <div class="cart-item-actions">
        <div class="quantity-control">
          <button onclick="updateCartQuantity(${product.id}, ${quantity - 1})">
            <i class="fas fa-minus"></i>
          </button>
          <span>${quantity}</span>
          <button onclick="updateCartQuantity(${product.id}, ${quantity + 1})">
            <i class="fas fa-plus"></i>
          </button>
        </div>
        <div class="cart-item-subtotal">R$ ${subtotal.toFixed(2)}</div>
        <button class="btn-icon text-danger" onclick="removeFromCart(${product.id})">
          <i class="fas fa-trash"></i>
        </button>
      </div>
    </div>
  `;
}

// pagina do carrinho com lista de itens, resumo e timer de expiracao - RN0044
export function CartPage() {
  const total = getCartTotal();

  return `
    ${Header()}
    <main>
      <section class="page-section">
        <div class="container">
          <h1 class="page-title">Carrinho de Compras</h1>

          ${
            state.cart.length === 0
              ? `
                <div class="empty-state">
                  <i class="fas fa-shopping-cart"></i>
                  <h3>Seu carrinho esta vazio</h3>
                  <p>Adicione produtos para continuar</p>
                  <button class="btn btn-primary" onclick="navigate('products')">
                    Ver Produtos
                  </button>
                </div>
              `
              : `
                ${state.isAuthenticated ? `
                  <div class="cart-timer-bar" id="cart-timer-bar">
                    <i class="fas fa-clock"></i>
                    <span>Tempo restante para finalizar: </span>
                    <strong id="cart-timer">Carregando...</strong>
                    <span class="text-muted" style="font-size: 0.75rem; margin-left: 0.5rem;">(RN0044 - Reserva de 30 minutos)</span>
                  </div>
                ` : ''}

                <div class="cart-content">
                  <div class="cart-items">
                    ${state.cart.map((item) => CartItem(item)).join("")}
                  </div>

                  <div class="cart-summary">
                    <h3>Resumo do Pedido</h3>
                    <div class="summary-row">
                      <span>Subtotal:</span>
                      <span>R$ ${total.toFixed(2)}</span>
                    </div>
                    <div class="summary-row">
                      <span>Frete:</span>
                      <span>A calcular</span>
                    </div>
                    <div class="divider"></div>
                    <div class="summary-row summary-total">
                      <span>Total:</span>
                      <span>R$ ${total.toFixed(2)}</span>
                    </div>
                    ${
                      state.isAuthenticated
                        ? `
                          <button class="btn btn-primary btn-block" onclick="navigate('checkout')">
                            Finalizar Compra
                          </button>
                        `
                        : `
                          <button class="btn btn-primary btn-block" onclick="navigate('login')">
                            Entrar para Finalizar
                          </button>
                        `
                    }
                    <button class="btn btn-outline btn-block" onclick="navigate('products')">
                      Continuar Comprando
                    </button>
                  </div>
                </div>
              `
          }
        </div>
      </section>
    </main>
    ${Footer()}
  `;
}

// inicia o timer de expiracao do carrinho - RN0044
export async function startCartTimer() {
  if (!state.isAuthenticated || state.cart.length === 0) return;

  try {
    const data = await apiRequest("/api/carrinho/tempo-restante");
    const segundos = Number(data);
    if (segundos > 0) {
      updateCartTimerDisplay(segundos);
      startCountdown(segundos);
    }
  } catch (err) {
    const timerEl = document.getElementById('cart-timer');
    if (timerEl) timerEl.textContent = '30:00';
  }
}

function startCountdown(totalSeconds) {
  if (state.cartTimerInterval) clearInterval(state.cartTimerInterval);

  let remaining = totalSeconds;
  state.cartTimerInterval = setInterval(() => {
    remaining--;
    if (remaining <= 0) {
      clearInterval(state.cartTimerInterval);
      showToast("Seu carrinho expirou! Os itens foram liberados.", "error");
      return;
    }
    // RN0044 - notificacao 5 minutos antes da expiracao
    if (remaining === 300) {
      showToast("Atencao: Seu carrinho expira em 5 minutos!", "error");
    }
    updateCartTimerDisplay(remaining);
  }, 1000);
}

function updateCartTimerDisplay(seconds) {
  const timerEl = document.getElementById('cart-timer');
  if (!timerEl) return;
  const min = Math.floor(seconds / 60);
  const sec = seconds % 60;
  timerEl.textContent = `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`;

  const bar = document.getElementById('cart-timer-bar');
  if (bar) {
    if (seconds <= 300) {
      bar.style.background = 'rgba(239, 68, 68, 0.1)';
      bar.style.borderColor = 'var(--danger)';
    }
  }
}
