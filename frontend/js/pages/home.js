import { state } from '../state.js';
import { Header } from '../components/header.js';
import { Footer } from '../components/footer.js';
import { ProductCard } from './products.js';

// pagina inicial com hero, categorias, produtos em destaque e diferenciais
export function HomePage() {
  const featuredProducts = state.products.slice(0, 6);

  return `
    ${Header()}
    <main>
      <!-- secao hero - banner principal -->
      <section class="hero">
        <div class="container">
          <div class="hero-content">
            <div class="hero-text">
              <span class="badge-primary">Novidade</span>
              <h1 class="hero-title">Trabalhe de Qualquer Lugar do Mundo</h1>
              <p class="hero-description">
                Equipamentos premium para nômades digitais. Notebooks, monitores portáteis,
                acessórios ergonômicos e muito mais para maximizar sua produtividade.
              </p>
              <div class="hero-actions">
                <button class="btn btn-primary btn-lg" onclick="navigate('products')">
                  Ver Produtos <i class="fas fa-arrow-right"></i>
                </button>
                <button class="btn btn-outline btn-lg" onclick="navigate('register')">
                  Criar Conta
                </button>
              </div>
              <div class="hero-stats">
                <div class="stat">
                  <div class="stat-value">500+</div>
                  <div class="stat-label">Produtos</div>
                </div>
                <div class="stat">
                  <div class="stat-value">98%</div>
                  <div class="stat-label">Satisfação</div>
                </div>
                <div class="stat">
                  <div class="stat-value">24h</div>
                  <div class="stat-label">Entrega</div>
                </div>
              </div>
            </div>
            <div class="hero-image">
              <img src="https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800" alt="Nômade Digital" />
            </div>
          </div>
        </div>
      </section>

      <!-- secao de categorias em destaque -->
      <section class="categories-section">
        <div class="container">
          <h2 class="section-title">Categorias em Destaque</h2>
          <div class="categories-grid">
            <div class="category-card">
              <div class="category-icon"><i class="fas fa-laptop"></i></div>
              <h3>Notebooks</h3>
              <p>Portáteis e potentes</p>
            </div>
            <div class="category-card">
              <div class="category-icon"><i class="fas fa-headphones"></i></div>
              <h3>Áudio</h3>
              <p>Headsets e fones</p>
            </div>
            <div class="category-card">
              <div class="category-icon"><i class="fas fa-keyboard"></i></div>
              <h3>Acessórios</h3>
              <p>Teclados e mouses</p>
            </div>
            <div class="category-card">
              <div class="category-icon"><i class="fas fa-chair"></i></div>
              <h3>Ergonomia</h3>
              <p>Cadeiras e suportes</p>
            </div>
          </div>
        </div>
      </section>

      <!-- secao de produtos em destaque (primeiros 6) -->
      <section class="products-section">
        <div class="container">
          <div class="section-header">
            <h2 class="section-title">Produtos em Destaque</h2>
            <button class="btn btn-outline" onclick="navigate('products')">
              Ver Todos <i class="fas fa-arrow-right"></i>
            </button>
          </div>
          <div class="products-grid">
            ${
              featuredProducts.length > 0
                ? featuredProducts.map((p) => ProductCard(p)).join("")
                : '<div class="empty-state">Nenhum produto cadastrado</div>'
            }
          </div>
        </div>
      </section>

      <!-- secao de diferenciais da loja -->
      <section class="features-section">
        <div class="container">
          <div class="features-grid">
            <div class="feature-card">
              <div class="feature-icon"><i class="fas fa-shipping-fast"></i></div>
              <h3>Entrega Rápida</h3>
              <p>Receba em até 24h nas principais capitais</p>
            </div>
            <div class="feature-card">
              <div class="feature-icon"><i class="fas fa-shield-alt"></i></div>
              <h3>Compra Segura</h3>
              <p>Pagamento criptografado e protegido</p>
            </div>
            <div class="feature-card">
              <div class="feature-icon"><i class="fas fa-exchange-alt"></i></div>
              <h3>Troca Grátis</h3>
              <p>30 dias para trocar qualquer produto</p>
            </div>
            <div class="feature-card">
              <div class="feature-icon"><i class="fas fa-headset"></i></div>
              <h3>Suporte 24/7</h3>
              <p>Atendimento sempre disponível</p>
            </div>
          </div>
        </div>
      </section>
    </main>
    ${Footer()}
  `;
}
