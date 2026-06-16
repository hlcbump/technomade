import { state } from './state.js';
import { apiRequest } from './api.js';
import { showToast } from './utils.js';
import { ensureAuthenticated } from './auth.js';
import { loadProducts } from './data.js';

// adiciona um produto ao carrinho local
export function addToCart(product, quantity = 1, navigateFn, renderFn) {
  if (!ensureAuthenticated(navigateFn)) return;

  // se o produto ja esta no carrinho, incrementa a quantidade
  const existing = state.cart.find((item) => item.product.id === product.id);
  if (existing) {
    existing.quantity += quantity;
  } else {
    state.cart.push({ product, quantity });
  }
  saveCartToStorage();
  showToast(`${product.nome} adicionado ao carrinho!`);
  renderFn();
}

// remove um produto do carrinho pelo id
export function removeFromCart(productId, renderFn) {
  state.cart = state.cart.filter((item) => item.product.id !== productId);
  saveCartToStorage();
  renderFn();
}

// atualiza a quantidade de um item no carrinho
export function updateCartQuantity(productId, quantity, navigateFn, renderFn) {
  if (!ensureAuthenticated(navigateFn)) return;

  const item = state.cart.find((item) => item.product.id === productId);
  if (item) {
    item.quantity = quantity;
    if (item.quantity <= 0) {
      removeFromCart(productId, renderFn);
    } else {
      saveCartToStorage();
      renderFn();
    }
  }
}

// calcula o valor total do carrinho (soma preco * quantidade)
export function getCartTotal() {
  return state.cart.reduce(
    (sum, item) => sum + item.product.valorVenda * item.quantity,
    0
  );
}

// conta o numero total de itens no carrinho
export function getCartCount() {
  return state.cart.reduce((sum, item) => sum + item.quantity, 0);
}

// salva o carrinho no localStorage do navegador
export function saveCartToStorage() {
  localStorage.setItem("technomade_cart", JSON.stringify(state.cart));
}

// carrega o carrinho salvo do localStorage
export function loadCartFromStorage() {
  const cartStr = localStorage.getItem("technomade_cart");
  if (cartStr) {
    try {
      state.cart = JSON.parse(cartStr);
    } catch (_) {
      state.cart = [];
    }
  }
}

// sincroniza o carrinho local com o backend (limpa e reenvia)
export async function syncCartToBackend() {
  if (!state.isAuthenticated) return;

  try {
    // remove todos os itens do carrinho no backend
    const backendCart = await apiRequest("/api/carrinho");
    for (const item of backendCart) {
      await apiRequest(`/api/carrinho/${item.id}`, { method: "DELETE" });
    }

    // adiciona os itens do localStorage ao backend
    for (const item of state.cart) {
      await apiRequest("/api/carrinho", {
        method: "POST",
        body: JSON.stringify({
          produtoId: item.product.id,
          quantidade: item.quantity,
        }),
      });
    }
  } catch (err) {
    console.error("Erro ao sincronizar carrinho:", err);
  }
}

// carrega o carrinho do backend e converte para o formato local
export async function loadCartFromBackend() {
  if (!state.isAuthenticated) return;

  try {
    const backendCart = await apiRequest("/api/carrinho");
    const products = await loadProducts();
    state.cart = backendCart.map((item) => ({
      product: products.find((p) => p.id === item.produtoId),
      quantity: item.quantidade,
    })).filter(item => item.product);
    saveCartToStorage();
  } catch (err) {
    console.error("Erro ao carregar carrinho:", err);
  }
}
