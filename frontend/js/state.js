// url base da API backend
export const API_BASE = "http://localhost:8080";
// chaves para salvar token e usuario no localStorage
export const AUTH_TOKEN_KEY = "technomade_token";
export const AUTH_USER_KEY = "technomade_user";

// estado global da aplicacao (armazena dados em memoria)
export const state = {
  currentView: "home", // pagina atual sendo exibida
  isAuthenticated: false, // se o usuario esta logado
  user: null, // dados do usuario logado
  cart: [], // itens no carrinho
  products: [], // lista de produtos
  categories: [], // lista de categorias
  users: [], // lista de usuarios (admin)
  stock: [], // lista de estoque (admin)
  movements: [], // lista de movimentacoes (admin)
  addresses: [], // enderecos do usuario
  cards: [], // cartoes do usuario
  shipping: null, // valor do frete calculado
  selectedAddressId: null, // endereco selecionado no checkout
  selectedCardId: null, // cartao selecionado no checkout
  checkoutCartoes: [], // cartoes adicionados ao pagamento [{cardId, valor}]
  // novas propriedades
  orders: [], // pedidos do usuario (cliente)
  adminOrders: [], // todos os pedidos (admin)
  adminOrderFilter: '', // filtro de status dos pedidos (admin)
  adminTrocas: [], // todas as trocas (admin)
  adminTrocaFilter: '', // filtro de status das trocas (admin)
  auditoriaLogs: [], // logs de auditoria (admin)
  notifications: [], // notificacoes do usuario
  notificationCount: 0, // contagem de nao lidas
  profileData: {}, // dados do perfil do usuario
  profileTab: 'dados', // aba ativa do perfil
  cupons: [], // cupons do usuario
  checkoutCupons: [], // cupons aplicados no checkout (multiplos)
  reportData: {}, // dados do relatorio de vendas
  reportCategorias: [], // vendas por categoria
  cartTimerInterval: null, // intervalo do timer do carrinho
  wishlistIds: [], // ids dos produtos na lista de desejos
};
