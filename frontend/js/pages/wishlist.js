import { state } from '../state.js';
import { showToast } from '../utils.js';
import { apiRequest } from '../api.js';
import { Header } from '../components/header.js';
import { Footer } from '../components/footer.js';

const WISHLIST_KEY = 'technomade_wishlist';

// pagina de lista de desejos - RF0061
export function WishlistPage() {
  if (!state.isAuthenticated) {
    return `
      ${Header()}
      <main>
        <section class="page-section">
          <div class="container">
            <div class="empty-state">
              <i class="fas fa-lock"></i>
              <h3>Acesso Restrito</h3>
              <p>Faca login para ver sua lista de desejos</p>
              <button class="btn btn-primary" onclick="navigate('login')">Entrar</button>
            </div>
          </div>
        </section>
      </main>
      ${Footer()}
    `;
  }

  const wishlist = getWishlist();
  const products = state.products.filter(p => wishlist.includes(p.id));

  return `
    ${Header()}
    <main>
      <section class="page-section">
        <div class="container">
          <h1 class="page-title">Lista de Desejos</h1>

          ${products.length > 0 ? `
            <div class="products-grid">
              ${products.map(p => WishlistProductCard(p)).join('')}
            </div>
          ` : `
            <div class="empty-state">
              <i class="fas fa-heart"></i>
              <h3>Sua lista de desejos esta vazia</h3>
              <p>Adicione produtos clicando no icone de coracao</p>
              <button class="btn btn-primary" onclick="navigate('products')">Ver Produtos</button>
            </div>
          `}
        </div>
      </section>
    </main>
    ${Footer()}
  `;
}

// card de produto na wishlist (com botao de remover)
function WishlistProductCard(product) {
  return `
    <div class="product-card">
      <div class="product-image" style="position: relative;">
        <img src="${product.imagemUrl || 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22300%22 height=%22200%22%3E%3Crect fill=%22%23f3f4f6%22 width=%22300%22 height=%22200%22/%3E%3Ctext fill=%22%236b7280%22 font-family=%22Arial%22 font-size=%2220%22 x=%2250%25%22 y=%2250%25%22 text-anchor=%22middle%22 dy=%22.3em%22%3EProduto%3C/text%3E%3C/svg%3E'}"
             alt="${product.nome}"
             onerror="if(!this.dataset.fallback){this.dataset.fallback='1';this.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22300%22 height=%22200%22%3E%3Crect fill=%22%23f3f4f6%22 width=%22300%22 height=%22200%22/%3E%3Ctext fill=%22%236b7280%22 font-family=%22Arial%22 font-size=%2220%22 x=%2250%25%22 y=%2250%25%22 text-anchor=%22middle%22 dy=%22.3em%22%3ESem Imagem%3C/text%3E%3C/svg%3E'}" />
        <button class="wishlist-btn wishlist-active" onclick="toggleWishlist(${product.id}); event.stopPropagation();" title="Remover da lista de desejos">
          <i class="fas fa-heart"></i>
        </button>
      </div>
      <div class="product-info">
        <h3 class="product-name">${product.nome}</h3>
        <p class="product-description">${product.descricao || 'Sem descricao'}</p>
        <div class="product-footer">
          <div class="product-price">R$ ${product.valorVenda.toFixed(2)}</div>
          <button class="btn btn-primary btn-sm" onclick='addToCart(${JSON.stringify(product)})'>
            <i class="fas fa-cart-plus"></i> Adicionar
          </button>
        </div>
      </div>
    </div>
  `;
}

// pega a wishlist - usa state (API) se autenticado, senao localStorage
export function getWishlist() {
  if (state.isAuthenticated && state.wishlistIds.length > 0) {
    return state.wishlistIds;
  }
  try {
    return JSON.parse(localStorage.getItem(WISHLIST_KEY)) || [];
  } catch (_) {
    return [];
  }
}

// verifica se produto esta na wishlist
export function isInWishlist(productId) {
  return getWishlist().includes(productId);
}

// adiciona/remove produto da wishlist - RF0061 com fallback localStorage
export async function toggleWishlist(productId) {
  const inList = isInWishlist(productId);

  // tenta via API se autenticado
  if (state.isAuthenticated) {
    try {
      if (inList) {
        await apiRequest(`/api/lista-desejos/${productId}`, { method: 'DELETE' });
        state.wishlistIds = state.wishlistIds.filter(id => id !== productId);
        showToast("Produto removido da lista de desejos");
      } else {
        await apiRequest(`/api/lista-desejos/${productId}`, { method: 'POST' });
        state.wishlistIds.push(productId);
        showToast("Produto adicionado a lista de desejos!");
      }
      // sincroniza localStorage como backup
      localStorage.setItem(WISHLIST_KEY, JSON.stringify(state.wishlistIds));
      window.render();
      return;
    } catch (err) {
      console.warn("API wishlist indisponivel, usando localStorage:", err);
    }
  }

  // fallback localStorage
  const wishlist = getWishlistFromStorage();
  const index = wishlist.indexOf(productId);

  if (index > -1) {
    wishlist.splice(index, 1);
    showToast("Produto removido da lista de desejos");
  } else {
    wishlist.push(productId);
    showToast("Produto adicionado a lista de desejos!");
  }

  localStorage.setItem(WISHLIST_KEY, JSON.stringify(wishlist));

  // atualiza state tambem para manter consistencia
  if (state.isAuthenticated) {
    state.wishlistIds = [...wishlist];
  }

  window.render();
}

// helper para pegar wishlist direto do localStorage
function getWishlistFromStorage() {
  try {
    return JSON.parse(localStorage.getItem(WISHLIST_KEY)) || [];
  } catch (_) {
    return [];
  }
}
