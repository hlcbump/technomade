import { state } from './state.js';
import { apiRequest } from './api.js';

// carrega a lista de produtos da API
export async function loadProducts() {
  try {
    const data = await apiRequest("/api/produtos");
    state.products = data || [];
    return state.products;
  } catch (err) {
    console.error("Erro ao carregar produtos:", err);
    return [];
  }
}

// carrega a lista de categorias da API
export async function loadCategories() {
  try {
    const data = await apiRequest("/api/categorias");
    state.categories = data || [];
    return state.categories;
  } catch (err) {
    console.error("Erro ao carregar categorias:", err);
    return [];
  }
}

// carrega os enderecos do usuario logado
export async function loadAddresses() {
  try {
    const data = await apiRequest("/api/enderecos");
    state.addresses = data || [];
    return state.addresses;
  } catch (err) {
    console.error("Erro ao carregar endereços:", err);
    state.addresses = [];
    return [];
  }
}

// carrega os cartoes de credito do usuario logado
export async function loadCards() {
  try {
    const data = await apiRequest("/api/cartoes");
    state.cards = data || [];
    return state.cards;
  } catch (err) {
    console.error("Erro ao carregar cartões:", err);
    state.cards = [];
    return [];
  }
}

// calcula o frete chamando a API com o id do usuario e endereco
export async function calculateShipping(addressId) {
  if (!state.user?.id || !addressId) {
    state.shipping = null;
    return;
  }

  try {
    const data = await apiRequest(
      `/api/compras/frete?enderecoEntregaId=${addressId}`
    );
    state.shipping = Number(data);
  } catch (err) {
    console.error("Erro ao calcular frete:", err);
    state.shipping = null;
  }
}

// carrega o estoque de todos os produtos (admin)
export async function loadStock() {
  try {
    const data = await apiRequest("/api/estoque");
    state.stock = data || [];
    return state.stock;
  } catch (err) {
    console.error("Erro ao carregar estoque:", err);
    state.stock = [];
    return [];
  }
}

// carrega as movimentacoes de estoque (admin)
export async function loadMovements() {
  try {
    const data = await apiRequest("/api/movimentacoes");
    state.movements = data || [];
    return state.movements;
  } catch (err) {
    console.error("Erro ao carregar movimentações:", err);
    state.movements = [];
    return [];
  }
}

// carrega a lista de usuarios da API (admin)
export async function loadUsers() {
  try {
    const data = await apiRequest("/api/usuarios?page=0&size=100");
    state.users = data.content || data || [];
    return state.users;
  } catch (err) {
    console.error("Erro ao carregar usuários:", err);
    state.users = [];
    return [];
  }
}

// carrega a lista de desejos do backend - RF0061
export async function loadWishlist() {
  try {
    const data = await apiRequest("/api/lista-desejos");
    // API retorna lista de produtos ou ids - extrai os ids
    state.wishlistIds = Array.isArray(data)
      ? data.map(item => item.id || item.produtoId || item)
      : [];
    // sincroniza localStorage como backup
    localStorage.setItem('technomade_wishlist', JSON.stringify(state.wishlistIds));
    return state.wishlistIds;
  } catch (err) {
    console.warn("API wishlist indisponivel, usando localStorage:", err);
    // fallback: carrega do localStorage
    try {
      state.wishlistIds = JSON.parse(localStorage.getItem('technomade_wishlist')) || [];
    } catch (_) {
      state.wishlistIds = [];
    }
    return state.wishlistIds;
  }
}

// carrega todos os dados necessarios para o checkout
export async function loadCheckoutData(ensureUserIdFn, syncCartFn) {
  await ensureUserIdFn();
  await syncCartFn();
  await loadAddresses();
  await loadCards();

  // seleciona o primeiro endereco se nenhum foi escolhido
  if (state.addresses.length > 0 && !state.selectedAddressId) {
    state.selectedAddressId = state.addresses[0].id;
  }

  // reseta cartoes e cupons do checkout anterior
  state.checkoutCartoes = [];
  state.checkoutCupons = [];

  // carrega cupons disponiveis do cliente
  if (state.user?.id) {
    try {
      const cupons = await apiRequest(`/api/cupons/${state.user.id}`);
      state.cupons = (cupons || []).filter(c => !c.usado);
    } catch (_) {
      state.cupons = [];
    }
  }

  // calcula o frete com base no endereco selecionado
  if (state.selectedAddressId) {
    await calculateShipping(state.selectedAddressId);
  }
}
