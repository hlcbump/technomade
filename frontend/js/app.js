// orquestrador principal - importa todos os modulos e registra funcoes no window

import { state } from './state.js';
import { checkAuth, ensureAuthenticated, login, register, logout, ensureUserId } from './auth.js';
import { addToCart, removeFromCart, updateCartQuantity, getCartTotal, getCartCount, saveCartToStorage, loadCartFromStorage, syncCartToBackend, loadCartFromBackend } from './cart.js';
import { loadProducts, loadCategories, loadAddresses, loadCards, loadCheckoutData, calculateShipping, loadStock, loadMovements, loadUsers, loadWishlist } from './data.js';
import { showToast } from './utils.js';
import { validarSenhaForte, validarConfirmacaoSenha, validarCpf, mascaraCpf, showFieldError, validarSenhaTempoReal } from './validators.js';
import { Header, toggleUserMenu, closeUserMenu } from './components/header.js';
import { Footer } from './components/footer.js';
import { HomePage } from './pages/home.js';
import { ProductsPage, ProductCard, filterProducts } from './pages/products.js';
import { CartPage, CartItem, startCartTimer } from './pages/cart.js';
import { CheckoutPage, handleAddressChange, handleCardChange, finalizarCompra, showAddressForm, hideAddressForm, handleCreateAddress, showCardForm, hideCardForm, handleCreateCard, applyCoupon, removeCoupon, addCheckoutCard, removeCheckoutCard } from './pages/checkout.js';
import { LoginPage, handleLogin } from './pages/login.js';
import { RegisterPage, handleRegister } from './pages/register.js';
import { AdminPage, AdminProductsTab, AdminUsersTab, AdminStockTab, AdminMovementsTab, AdminOrdersTab, AdminTrocasTab, AdminAuditoriaTab, switchAdminTab, showProductForm, hideProductForm, handleCreateProduct, handleUpdateProduct, editProduct, deleteProduct, showUserForm, hideUserForm, handleCreateUser, handleUpdateUser, editUser, deleteUser, showMovementForm, hideMovementForm, handleCreateMovement, showInactivateProductForm, showActivateProductForm, handleInactivateProduct, handleActivateProduct, toggleUserStatus, filterUsers, updateOrderStatus, filterAdminOrders, toggleAdminOrderDetails, toggleAdminTrocaDetails, filterAdminTrocas, authorizeTroca, denyTroca, confirmTrocaReceived, handleFilterAuditoria, showCupomForm, hideCupomForm, handleCreateCupom } from './pages/admin.js';
import { ProfilePage, switchProfileTab, loadProfileData, handleUpdateProfile, handleChangePassword, showProfileAddressForm, hideProfileAddressForm, handleProfileAddress, editProfileAddress, deleteProfileAddress, showProfileCardForm, hideProfileCardForm, handleProfileCard, showEditCardForm, handleEditCard, deleteProfileCard } from './pages/profile.js';
import { OrdersPage, toggleOrderDetails, showExchangeForm, hideExchangeForm, handleRequestExchange, loadOrders } from './pages/orders.js';
import { NotificationsPage, loadNotifications, loadNotificationCount, markNotificationRead, markAllNotificationsRead } from './pages/notifications.js';
import { WishlistPage, toggleWishlist } from './pages/wishlist.js';
import { ReportsPage, handleLoadReport, initReportMasks } from './pages/reports.js';
import { ChatbotWidget, initChatbot } from './components/chatbot.js';

// navega entre as paginas da SPA atualizando o state e re-renderizando
async function navigate(view) {
  if (view === "checkout" && !ensureAuthenticated(navigate)) return;
  if (view === "orders" && !ensureAuthenticated(navigate)) return;
  if (view === "profile" && !ensureAuthenticated(navigate)) return;
  if (view === "wishlist" && !ensureAuthenticated(navigate)) return;
  if (view === "notifications" && !ensureAuthenticated(navigate)) return;

  state.currentView = view;
  location.hash = view;

  if (view === "checkout") {
    await loadCheckoutData(ensureUserId, syncCartToBackend);
  } else if (view === "profile") {
    await loadProfileData();
    await loadAddresses();
    await loadCards();
  } else if (view === "orders") {
    await ensureUserId();
    await loadOrders();
  } else if (view === "notifications") {
    await loadNotifications();
  } else if (view === "wishlist") {
    await loadWishlist();
  }

  render();
  window.scrollTo(0, 0);

  // inicia timer do carrinho se estiver na pagina do carrinho
  if (view === "cart") {
    startCartTimer();
  }
}

// funcao principal de renderizacao - decide qual pagina exibir com base no state
function render() {
  const app = document.getElementById("app");

  let content = "";
  switch (state.currentView) {
    case "home":
      content = HomePage();
      break;
    case "products":
      content = ProductsPage();
      break;
    case "cart":
      content = CartPage();
      break;
    case "login":
      content = LoginPage();
      break;
    case "register":
      content = RegisterPage();
      break;
    case "admin":
      content = AdminPage();
      break;
    case "checkout":
      content = CheckoutPage();
      break;
    case "profile":
      content = ProfilePage();
      break;
    case "orders":
      content = OrdersPage();
      break;
    case "notifications":
      content = NotificationsPage();
      break;
    case "wishlist":
      content = WishlistPage();
      break;
    case "reports":
      content = ReportsPage();
      break;
    default:
      content = HomePage();
  }

  // injeta o HTML gerado na div #app
  app.innerHTML = content;

  if (state.currentView === 'reports') {
    initReportMasks();
  }
}

// inicializa a aplicacao ao carregar a pagina
async function init() {
  checkAuth(); // verifica se ha sessao salva
  loadCartFromStorage(); // carrega carrinho do localStorage
  await loadProducts(); // carrega produtos da API
  await loadCategories(); // carrega categorias da API

  // sincroniza carrinho com o backend se estiver logado (apenas clientes)
  if (state.isAuthenticated) {
    if (state.user?.role !== 'ADMIN') {
      await syncCartToBackend();
    }
    // carrega contagem de notificacoes nao lidas
    loadNotificationCount();
    // carrega lista de desejos do backend - RF0061
    loadWishlist();
  }

  // restaura a pagina a partir do hash da URL
  const hash = location.hash.replace('#', '');
  if (hash) {
    await navigate(hash);
  }

  render(); // renderiza a pagina inicial

  // inicializa o chatbot de recomendacao com IA
  if (!document.getElementById('chatbot-container')) {
    const chatbotDiv = document.createElement('div');
    chatbotDiv.innerHTML = ChatbotWidget();
    document.body.appendChild(chatbotDiv);
    initChatbot();
  }

  // navega ao mudar o hash (botoes voltar/avancar do navegador)
  window.addEventListener('hashchange', () => {
    const h = location.hash.replace('#', '');
    if (h && h !== state.currentView) {
      navigate(h);
    }
  });

  // fecha o dropdown do usuario ao clicar fora dele
  document.addEventListener("click", (e) => {
    if (!e.target.closest(".user-menu")) {
      closeUserMenu();
    }
  });
}

// expoe funcoes no escopo global para serem chamadas pelo HTML (onclick)
window.navigate = navigate;
window.render = render;
window.addToCart = (product, quantity) => addToCart(product, quantity, navigate, render);
window.removeFromCart = (productId) => removeFromCart(productId, render);
window.updateCartQuantity = (productId, quantity) => updateCartQuantity(productId, quantity, navigate, render);
window.handleLogin = (e) => handleLogin(e, navigate);
window.handleRegister = (e) => handleRegister(e, navigate);
window.logout = () => { logout(); location.reload(true); };
window.toggleUserMenu = toggleUserMenu;
window.closeUserMenu = closeUserMenu;
window.filterProducts = filterProducts;
window.switchAdminTab = switchAdminTab;
window.showProductForm = showProductForm;
window.hideProductForm = hideProductForm;
window.handleCreateProduct = handleCreateProduct;
window.handleUpdateProduct = handleUpdateProduct;
window.editProduct = editProduct;
window.deleteProduct = deleteProduct;
window.showAddressForm = showAddressForm;
window.hideAddressForm = hideAddressForm;
window.handleCreateAddress = (e) => handleCreateAddress(e, render);
window.showCardForm = showCardForm;
window.hideCardForm = hideCardForm;
window.handleCreateCard = (e) => handleCreateCard(e, render);
window.handleAddressChange = (value) => handleAddressChange(value, render);
window.handleCardChange = handleCardChange;
window.addCheckoutCard = addCheckoutCard;
window.removeCheckoutCard = removeCheckoutCard;
window.finalizarCompra = () => finalizarCompra(navigate, render);
window.showMovementForm = showMovementForm;
window.hideMovementForm = hideMovementForm;
window.handleCreateMovement = handleCreateMovement;
window.showUserForm = showUserForm;
window.hideUserForm = hideUserForm;
window.handleCreateUser = handleCreateUser;
window.handleUpdateUser = handleUpdateUser;
window.editUser = editUser;
window.deleteUser = deleteUser;
window.validarSenhaTempoReal = validarSenhaTempoReal;
window.validarSenhaForte = validarSenhaForte;
window.validarConfirmacaoSenha = validarConfirmacaoSenha;
window.showFieldError = showFieldError;
window.validarCpf = validarCpf;
window.mascaraCpf = mascaraCpf;
window.applyCoupon = applyCoupon;
window.removeCoupon = removeCoupon;
window.switchProfileTab = switchProfileTab;
window.handleUpdateProfile = handleUpdateProfile;
window.handleChangePassword = handleChangePassword;
window.showProfileAddressForm = showProfileAddressForm;
window.hideProfileAddressForm = hideProfileAddressForm;
window.handleProfileAddress = handleProfileAddress;
window.editProfileAddress = editProfileAddress;
window.deleteProfileAddress = deleteProfileAddress;
window.showProfileCardForm = showProfileCardForm;
window.hideProfileCardForm = hideProfileCardForm;
window.handleProfileCard = handleProfileCard;
window.showEditCardForm = showEditCardForm;
window.handleEditCard = handleEditCard;
window.deleteProfileCard = deleteProfileCard;
window.toggleOrderDetails = toggleOrderDetails;
window.showExchangeForm = showExchangeForm;
window.hideExchangeForm = hideExchangeForm;
window.handleRequestExchange = handleRequestExchange;
window.markNotificationRead = markNotificationRead;
window.markAllNotificationsRead = markAllNotificationsRead;
window.toggleWishlist = (productId) => toggleWishlist(productId, render);
window.handleLoadReport = handleLoadReport;
window.showInactivateProductForm = showInactivateProductForm;
window.showActivateProductForm = showActivateProductForm;
window.handleInactivateProduct = handleInactivateProduct;
window.handleActivateProduct = handleActivateProduct;
window.toggleUserStatus = toggleUserStatus;
window.filterUsers = filterUsers;
window.updateOrderStatus = updateOrderStatus;
window.filterAdminOrders = filterAdminOrders;
window.toggleAdminOrderDetails = toggleAdminOrderDetails;
window.toggleAdminTrocaDetails = toggleAdminTrocaDetails;
window.filterAdminTrocas = filterAdminTrocas;
window.authorizeTroca = authorizeTroca;
window.denyTroca = denyTroca;
window.confirmTrocaReceived = confirmTrocaReceived;
window.handleFilterAuditoria = handleFilterAuditoria;
window.showCupomForm = showCupomForm;
window.hideCupomForm = hideCupomForm;
window.handleCreateCupom = handleCreateCupom;

// inicia a aplicacao
init();