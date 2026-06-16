import { state } from '../state.js';
import { Header } from '../components/header.js';
import { Footer } from '../components/footer.js';
import { isInWishlist } from './wishlist.js';

// componente de card de produto reutilizavel com botao de wishlist - RF0061
export function ProductCard(product) {
  const inWishlist = state.isAuthenticated && isInWishlist(product.id);

  return `
    <div class="product-card">
      <div class="product-image" style="position: relative;">
        <img src="${product.imagemUrl || 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22300%22 height=%22200%22%3E%3Crect fill=%22%23f3f4f6%22 width=%22300%22 height=%22200%22/%3E%3Ctext fill=%22%236b7280%22 font-family=%22Arial%22 font-size=%2220%22 x=%2250%25%22 y=%2250%25%22 text-anchor=%22middle%22 dy=%22.3em%22%3EProduto%3C/text%3E%3C/svg%3E'}"
             alt="${product.nome}"
             onerror="if(!this.dataset.fallback){this.dataset.fallback='1';this.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22300%22 height=%22200%22%3E%3Crect fill=%22%23f3f4f6%22 width=%22300%22 height=%22200%22/%3E%3Ctext fill=%22%236b7280%22 font-family=%22Arial%22 font-size=%2220%22 x=%2250%25%22 y=%2250%25%22 text-anchor=%22middle%22 dy=%22.3em%22%3ESem Imagem%3C/text%3E%3C/svg%3E'}" />
        ${state.isAuthenticated ? `
          <button class="wishlist-btn ${inWishlist ? 'wishlist-active' : ''}" onclick="toggleWishlist(${product.id}); event.stopPropagation();" title="${inWishlist ? 'Remover da' : 'Adicionar a'} lista de desejos">
            <i class="fas fa-heart"></i>
          </button>
        ` : ''}
      </div>
      <div class="product-info">
        <h3 class="product-name">${product.nome}</h3>
        <p class="product-description">${product.descricao || 'Sem descricao'}</p>
        ${product.categorias && product.categorias.length > 0 ? `
          <div class="product-categories">
            ${product.categorias.map(c => `<span class="product-category-tag">${c.nome}</span>`).join('')}
          </div>
        ` : ''}
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

// pagina de listagem de todos os produtos com filtros avancados - RF0015
export function ProductsPage() {
  return `
    ${Header()}
    <main>
      <section class="page-section">
        <div class="container">
          <h1 class="page-title">Todos os Produtos</h1>

          <div class="products-filters">
            <div class="products-filters-row">
              <div class="search-box">
                <i class="fas fa-search"></i>
                <input type="text" placeholder="Buscar produtos..." id="search-input"
                       oninput="filterProducts()" />
              </div>

              <div class="filter-group">
                <select id="filter-categoria" onchange="filterProducts()" class="filter-select">
                  <option value="">Todas as Categorias</option>
                  ${state.categories.map(c => `<option value="${c.id}">${c.nome}</option>`).join('')}
                </select>
              </div>

              <div class="filter-group">
                <select id="filter-marca" onchange="filterProducts()" class="filter-select">
                  <option value="">Todas as Marcas</option>
                  ${[...new Set(state.products.map(p => p.marca).filter(Boolean))].map(m => `<option value="${m}">${m}</option>`).join('')}
                </select>
              </div>

              <div class="filter-group">
                <select id="filter-preco" onchange="filterProducts()" class="filter-select">
                  <option value="">Faixa de Preco</option>
                  <option value="0-50">Ate R$ 50</option>
                  <option value="50-100">R$ 50 - R$ 100</option>
                  <option value="100-500">R$ 100 - R$ 500</option>
                  <option value="500-1000">R$ 500 - R$ 1.000</option>
                  <option value="1000-99999">Acima de R$ 1.000</option>
                </select>
              </div>
            </div>
          </div>

          <div class="products-grid" id="products-container">
            ${
              state.products.length > 0
                ? state.products.filter(p => p.ativo !== false).map((p) => ProductCard(p)).join("")
                : '<div class="empty-state">Nenhum produto encontrado</div>'
            }
          </div>
        </div>
      </section>
    </main>
    ${Footer()}
  `;
}

// filtra produtos por nome, descricao, categoria, marca e faixa de preco - RF0015
export function filterProducts() {
  const searchInput = document.getElementById("search-input");
  const catSelect = document.getElementById("filter-categoria");
  const marcaSelect = document.getElementById("filter-marca");
  const precoSelect = document.getElementById("filter-preco");

  const query = searchInput ? searchInput.value.toLowerCase() : '';
  const catId = catSelect ? Number(catSelect.value) : 0;
  const marca = marcaSelect ? marcaSelect.value : '';
  const precoRange = precoSelect ? precoSelect.value : '';

  let filtered = state.products.filter(p => p.ativo !== false);

  if (query) {
    filtered = filtered.filter(
      p => p.nome.toLowerCase().includes(query) ||
           (p.descricao && p.descricao.toLowerCase().includes(query))
    );
  }

  if (catId) {
    filtered = filtered.filter(
      p => p.categorias && p.categorias.some(c => c.id === catId)
    );
  }

  if (marca) {
    filtered = filtered.filter(p => p.marca === marca);
  }

  if (precoRange) {
    const [min, max] = precoRange.split('-').map(Number);
    filtered = filtered.filter(p => p.valorVenda >= min && p.valorVenda <= max);
  }

  const container = document.getElementById("products-container");
  container.innerHTML =
    filtered.length > 0
      ? filtered.map((p) => ProductCard(p)).join("")
      : '<div class="empty-state">Nenhum produto encontrado para os filtros selecionados</div>';
}
