import { state } from '../state.js';
import { getCartCount } from '../cart.js';

// componente do cabecalho com logo, navegacao e acoes do usuario
export function Header() {
  const cartCount = getCartCount();
  const notifCount = state.notificationCount || 0;

  return `
    <header class="header">
      <div class="container">
        <div class="header-content">
          <div class="brand" onclick="navigate('home')">
            <div class="logo">
              <i class="fas fa-laptop-code"></i>
            </div>
            <div class="brand-text">
              <div class="brand-name">Technomade</div>
              <div class="brand-tagline">Digital Nomad</div>
            </div>
          </div>

          <nav class="nav">
            <a href="#" onclick="navigate('home'); return false;">Home</a>
            <a href="#" onclick="navigate('products'); return false;">Produtos</a>
            ${
              state.isAuthenticated && state.user.role === "ADMIN"
                ? `<a href="#" onclick="navigate('admin'); return false;">Administracao</a>
                   <a href="#" onclick="navigate('reports'); return false;">Relatorios</a>`
                : ""
            }
          </nav>

          <div class="header-actions">
            ${state.isAuthenticated ? `
              <button class="btn-icon" onclick="navigate('wishlist'); return false;" title="Lista de Desejos">
                <i class="fas fa-heart"></i>
              </button>
              <button class="btn-icon" onclick="navigate('notifications'); return false;" title="Notificacoes">
                <i class="fas fa-bell"></i>
                ${notifCount > 0 ? `<span class="badge">${notifCount}</span>` : ""}
              </button>
            ` : ''}

            <button class="btn-icon" onclick="navigate('cart'); return false;" title="Carrinho">
              <i class="fas fa-shopping-cart"></i>
              ${cartCount > 0 ? `<span class="badge">${cartCount}</span>` : ""}
            </button>

            ${
              state.isAuthenticated
                ? `
                  <div class="user-menu">
                    <button class="btn-icon" onclick="toggleUserMenu(); return false;" title="Menu do Usuario">
                      <i class="fas fa-user"></i>
                    </button>
                    <div class="user-dropdown" id="user-dropdown">
                      <div class="user-info">
                        <div class="user-email">${state.user.email}</div>
                        <div class="user-role">${state.user.role}</div>
                      </div>
                      <div class="divider"></div>
                      <a href="#" onclick="navigate('profile'); closeUserMenu(); return false;">
                        <i class="fas fa-user"></i> Meu Perfil
                      </a>
                      <a href="#" onclick="navigate('orders'); closeUserMenu(); return false;">
                        <i class="fas fa-box"></i> Meus Pedidos
                      </a>
                      <a href="#" onclick="navigate('wishlist'); closeUserMenu(); return false;">
                        <i class="fas fa-heart"></i> Lista de Desejos
                      </a>
                      <a href="#" onclick="navigate('notifications'); closeUserMenu(); return false;">
                        <i class="fas fa-bell"></i> Notificacoes ${notifCount > 0 ? `<span class="badge" style="position:static; margin-left: 0.5rem;">${notifCount}</span>` : ''}
                      </a>
                      <div class="divider"></div>
                      <a href="#" onclick="logout(); closeUserMenu(); return false;" class="text-danger">
                        <i class="fas fa-sign-out-alt"></i> Sair
                      </a>
                    </div>
                  </div>
                `
                : `
                  <button class="btn btn-outline" onclick="navigate('login'); return false;">
                    Entrar
                  </button>
                  <button class="btn btn-primary" onclick="navigate('register'); return false;">
                    Cadastrar
                  </button>
                `
            }
          </div>
        </div>
      </div>
    </header>
  `;
}

// abre/fecha o menu dropdown do usuario
export function toggleUserMenu() {
  const dropdown = document.getElementById("user-dropdown");
  dropdown.classList.toggle("show");
}

// fecha o menu dropdown do usuario
export function closeUserMenu() {
  const dropdown = document.getElementById("user-dropdown");
  if (dropdown) dropdown.classList.remove("show");
}
