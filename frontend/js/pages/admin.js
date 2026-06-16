import { state } from '../state.js';
import { apiRequest } from '../api.js';
import { showToast } from '../utils.js';
import { Header } from '../components/header.js';
import { Footer } from '../components/footer.js';
import { loadProducts, loadStock, loadMovements, loadUsers, loadCategories } from '../data.js';
import { validarSenhaForte, validarConfirmacaoSenha, validarCpf, mascaraCpf, showFieldError } from '../validators.js';

// mapeamento de status de compra
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

// pagina do painel administrativo com abas (produtos, usuarios, estoque, etc)
export function AdminPage() {
  // bloqueia acesso se nao for admin
  if (!state.isAuthenticated || state.user.role !== "ADMIN") {
    return `
      ${Header()}
      <main>
        <section class="page-section">
          <div class="container">
            <div class="empty-state">
              <i class="fas fa-lock"></i>
              <h3>Acesso Negado</h3>
              <p>Você precisa ser administrador para acessar esta página</p>
              <button class="btn btn-primary" onclick="navigate('home')">Voltar ao Início</button>
            </div>
          </div>
        </section>
      </main>
      ${Footer()}
    `;
  }

  return `
    ${Header()}
    <main>
      <section class="page-section">
        <div class="container">
          <h1 class="page-title">Painel Administrativo</h1>

          <div class="admin-tabs">
            <button class="tab-btn active" onclick="switchAdminTab('products', event)">
              <i class="fas fa-box"></i> Produtos
            </button>
            <button class="tab-btn" onclick="switchAdminTab('users', event)">
              <i class="fas fa-users"></i> Usuários
            </button>
            <button class="tab-btn" onclick="switchAdminTab('stock', event)">
              <i class="fas fa-warehouse"></i> Estoque
            </button>
            <button class="tab-btn" onclick="switchAdminTab('movements', event)">
              <i class="fas fa-exchange-alt"></i> Movimentações
            </button>
            <button class="tab-btn" onclick="switchAdminTab('orders', event)">
              <i class="fas fa-shopping-bag"></i> Pedidos
            </button>
            <button class="tab-btn" onclick="switchAdminTab('trocas', event)">
              <i class="fas fa-undo"></i> Trocas
            </button>
            <button class="tab-btn" onclick="switchAdminTab('cupons', event)">
              <i class="fas fa-ticket-alt"></i> Cupons
            </button>
            <button class="tab-btn" onclick="switchAdminTab('auditoria', event)">
              <i class="fas fa-clipboard-list"></i> Auditoria
            </button>
          </div>

          <div id="admin-content">
            ${AdminProductsTab()}
          </div>
        </div>
      </section>
    </main>
    ${Footer()}
  `;
}

// aba de gerenciamento de produtos no admin (tabela + formulario) - RF0011, RF0014, RF0012, RF0016
export function AdminProductsTab() {
  return `
    <div class="admin-section">
      <div class="admin-header">
        <h2>Gerenciar Produtos</h2>
        <button class="btn btn-primary" onclick="showProductForm()">
          <i class="fas fa-plus"></i> Novo Produto
        </button>
      </div>

      <div id="product-form-container"></div>

      <div class="admin-table">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Nome</th>
              <th>Marca</th>
              <th>Categorias</th>
              <th>Preco</th>
              <th>Status</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            ${
              state.products.length > 0
                ? state.products
                    .map(
                      (p) => `
                    <tr>
                      <td>#${p.id}</td>
                      <td>${p.nome}</td>
                      <td>${p.marca || '-'}</td>
                      <td>${p.categorias && p.categorias.length > 0 ? p.categorias.map(c => c.nome).join(', ') : '-'}</td>
                      <td>R$ ${p.valorVenda.toFixed(2)}</td>
                      <td>
                        <span class="badge-status ${p.ativo !== false ? 'status-active' : 'status-inactive'}">
                          ${p.ativo !== false ? 'Ativo' : 'Inativo'}
                        </span>
                      </td>
                      <td>
                        <button class="btn-icon" onclick="editProduct(${p.id})" title="Editar">
                          <i class="fas fa-edit"></i>
                        </button>
                        ${p.ativo !== false
                          ? `<button class="btn-icon text-danger" onclick="showInactivateProductForm(${p.id})" title="Inativar">
                               <i class="fas fa-ban"></i>
                             </button>`
                          : `<button class="btn-icon" onclick="showActivateProductForm(${p.id})" title="Ativar" style="color: var(--success);">
                               <i class="fas fa-check-circle"></i>
                             </button>`
                        }
                        <button class="btn-icon text-danger" onclick="deleteProduct(${p.id})" title="Excluir">
                          <i class="fas fa-trash"></i>
                        </button>
                      </td>
                    </tr>
                  `
                    )
                    .join("")
                : '<tr><td colspan="7" class="text-center">Nenhum produto cadastrado</td></tr>'
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// aba de gerenciamento de usuarios no admin (tabela + formulario + filtros) - RF0023, RF0024
export function AdminUsersTab() {
  return `
    <div class="admin-section">
      <div class="admin-header">
        <h2>Gerenciar Usuarios</h2>
        <button class="btn btn-primary" onclick="showUserForm()">
          <i class="fas fa-plus"></i> Novo Usuario
        </button>
      </div>

      <div class="products-filters" style="margin-bottom: 1rem;">
        <div class="search-box">
          <i class="fas fa-search"></i>
          <input type="text" placeholder="Buscar por nome, email ou CPF..." id="user-search-input"
                 oninput="filterUsers()" />
        </div>
      </div>

      <div id="user-form-container"></div>

      <div class="admin-table" id="users-table-container">
        ${renderUsersTable(state.users)}
      </div>
    </div>
  `;
}

// renderiza tabela de usuarios (reutilizavel para filtro)
function renderUsersTable(users) {
  return `
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>Nome</th>
          <th>Email</th>
          <th>Role</th>
          <th>CPF</th>
          <th>Status</th>
          <th>Acoes</th>
        </tr>
      </thead>
      <tbody>
        ${
          users.length > 0
            ? users
                .map(
                  (u) => `
                <tr>
                  <td>#${u.id}</td>
                  <td>${u.nome}</td>
                  <td>${u.email}</td>
                  <td><span class="badge-role ${u.role.toLowerCase()}">${u.role}</span></td>
                  <td>${u.cpf || '-'}</td>
                  <td>
                    <span class="badge-status ${u.ativo !== false ? 'status-active' : 'status-inactive'}">
                      ${u.ativo !== false ? 'Ativo' : 'Inativo'}
                    </span>
                  </td>
                  <td>
                    <button class="btn-icon" onclick="editUser(${u.id})" title="Editar">
                      <i class="fas fa-edit"></i>
                    </button>
                    ${u.ativo !== false
                      ? `<button class="btn-icon text-danger" onclick="toggleUserStatus(${u.id}, false)" title="Inativar">
                           <i class="fas fa-ban"></i>
                         </button>`
                      : `<button class="btn-icon" onclick="toggleUserStatus(${u.id}, true)" title="Ativar" style="color: var(--success);">
                           <i class="fas fa-check-circle"></i>
                         </button>`
                    }
                  </td>
                </tr>
              `
                )
                .join("")
            : '<tr><td colspan="7" class="text-center">Nenhum usuario encontrado</td></tr>'
        }
      </tbody>
    </table>
  `;
}

// aba de visualizacao do estoque no admin
export function AdminStockTab() {
  return `
    <div class="admin-section">
      <div class="admin-header">
        <h2>Estoque</h2>
      </div>

      <div class="admin-table">
        <table>
          <thead>
            <tr>
              <th>Produto</th>
              <th>Quantidade</th>
              <th>Valor Custo</th>
              <th>Fornecedor</th>
              <th>Última Entrada</th>
            </tr>
          </thead>
          <tbody>
            ${
              state.stock.length > 0
                ? state.stock
                    .map((s) => `
                      <tr>
                        <td>${s.produto ? s.produto.nome : '-'}</td>
                        <td>${s.quantidade ?? '-'}</td>
                        <td>${s.valorCusto != null ? `R$ ${Number(s.valorCusto).toFixed(2)}` : '-'}</td>
                        <td>${s.fornecedor || '-'}</td>
                        <td>${s.ultimaEntrada ? new Date(s.ultimaEntrada).toLocaleString() : '-'}</td>
                      </tr>
                    `)
                    .join("")
                : '<tr><td colspan="5" class="text-center">Nenhum item em estoque</td></tr>'
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// aba de movimentacoes de estoque no admin (entrada/saida)
export function AdminMovementsTab() {
  return `
    <div class="admin-section">
      <div class="admin-header">
        <h2>Movimentações de Estoque</h2>
        <button class="btn btn-primary" onclick="showMovementForm()">
          <i class="fas fa-plus"></i> Nova Movimentação
        </button>
      </div>

      <div id="movement-form-container"></div>

      <div class="admin-table">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Produto</th>
              <th>Tipo</th>
              <th>Quantidade</th>
              <th>Valor Custo</th>
              <th>Fornecedor</th>
              <th>Data</th>
            </tr>
          </thead>
          <tbody>
            ${
              state.movements.length > 0
                ? state.movements
                    .map((m) => `
                      <tr>
                        <td>#${m.id}</td>
                        <td>${m.produto ? m.produto.nome : '-'}</td>
                        <td>${m.tipo}</td>
                        <td>${m.quantidade}</td>
                        <td>${m.valorCusto != null ? `R$ ${Number(m.valorCusto).toFixed(2)}` : '-'}</td>
                        <td>${m.fornecedor || '-'}</td>
                        <td>${m.dataHora ? new Date(m.dataHora).toLocaleString() : '-'}</td>
                      </tr>
                    `)
                    .join("")
                : '<tr><td colspan="7" class="text-center">Nenhuma movimentação registrada</td></tr>'
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// troca entre as abas do painel admin e carrega os dados necessarios
export async function switchAdminTab(tab, evt) {
  // atualiza a aba ativa visualmente
  const buttons = document.querySelectorAll(".tab-btn");
  buttons.forEach((btn) => btn.classList.remove("active"));
  evt.target.closest(".tab-btn").classList.add("active");

  const content = document.getElementById("admin-content");
  if (tab === "products") {
    content.innerHTML = AdminProductsTab();
  } else if (tab === "users") {
    content.innerHTML = '<div class="loading">Carregando usuários...</div>';
    await loadUsers();
    content.innerHTML = AdminUsersTab();
  } else if (tab === "stock") {
    content.innerHTML = '<div class="loading">Carregando estoque...</div>';
    await loadStock();
    content.innerHTML = AdminStockTab();
  } else if (tab === "movements") {
    content.innerHTML = '<div class="loading">Carregando movimentações...</div>';
    await loadMovements();
    content.innerHTML = AdminMovementsTab();
  } else if (tab === "orders") {
    content.innerHTML = '<div class="loading">Carregando pedidos...</div>';
    await loadAllOrders();
    content.innerHTML = AdminOrdersTab();
  } else if (tab === "trocas") {
    content.innerHTML = '<div class="loading">Carregando trocas...</div>';
    await loadAllTrocas();
    content.innerHTML = AdminTrocasTab();
  } else if (tab === "cupons") {
    content.innerHTML = '<div class="loading">Carregando cupons...</div>';
    await loadAllCupons();
    content.innerHTML = AdminCuponsTab();
  } else if (tab === "auditoria") {
    content.innerHTML = '<div class="loading">Carregando auditoria...</div>';
    await loadAuditoria();
    content.innerHTML = AdminAuditoriaTab();
  }
}

// exibe o formulario para criar/editar produto - RF0011, RF0014, RN0012
export function showProductForm(productId = null) {
  const product = productId ? state.products.find(p => p.id === productId) : null;
  const isEdit = !!product;
  const selectedCats = isEdit && product.categorias ? product.categorias.map(c => c.id) : [];

  const container = document.getElementById("product-form-container");
  container.innerHTML = `
    <div class="admin-form">
      <h3>${isEdit ? 'Editar' : 'Novo'} Produto</h3>
      <form onsubmit="${isEdit ? `handleUpdateProduct(event, ${product.id})` : 'handleCreateProduct(event)'}">
        <div class="form-row">
          <div class="form-group">
            <label>Nome *</label>
            <input type="text" name="nome" value="${isEdit ? product.nome || '' : ''}" required />
          </div>
          <div class="form-group">
            <label>Marca *</label>
            <input type="text" name="marca" value="${isEdit ? product.marca || '' : ''}" required />
          </div>
        </div>
        <div class="form-group">
          <label>Descricao *</label>
          <textarea name="descricao" rows="3" required>${isEdit ? product.descricao || '' : ''}</textarea>
        </div>
        <div class="form-group">
          <label>URL da Imagem</label>
          <input type="url" name="imagemUrl" value="${isEdit ? product.imagemUrl || '' : ''}" />
        </div>
        <div class="form-group">
          <label>Categorias</label>
          <div class="categories-checkboxes" style="display: flex; flex-wrap: wrap; gap: 0.5rem;">
            ${state.categories.map(cat => `
              <label style="display: flex; align-items: center; gap: 0.25rem; padding: 0.25rem 0.5rem; background: var(--gray-light); border-radius: var(--radius-sm); font-size: 0.813rem;">
                <input type="checkbox" name="categorias" value="${cat.id}" ${selectedCats.includes(cat.id) ? 'checked' : ''} />
                ${cat.nome}
              </label>
            `).join('')}
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Valor Custo (R$) *</label>
            <input type="number" name="valorCusto" step="0.01" value="${isEdit ? product.valorCusto || '' : ''}" required />
          </div>
          <div class="form-group">
            <label>Valor Venda (R$) *</label>
            <input type="number" name="valorVenda" step="0.01" value="${isEdit ? product.valorVenda || '' : ''}" required />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Codigo de Barras *</label>
            <input type="text" name="codigoBarras" value="${isEdit ? product.codigoBarras || '' : ''}" required />
          </div>
          <div class="form-group">
            <label>Grupo Precificacao ID</label>
            <input type="number" name="grupoPrecificacaoId" value="${isEdit && product.grupoPrecificacao ? product.grupoPrecificacao.id : '1'}" required />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Altura (cm)</label>
            <input type="number" name="altura" step="0.01" value="${isEdit ? product.altura || '10' : '10'}" required />
          </div>
          <div class="form-group">
            <label>Largura (cm)</label>
            <input type="number" name="largura" step="0.01" value="${isEdit ? product.largura || '10' : '10'}" required />
          </div>
          <div class="form-group">
            <label>Profundidade (cm)</label>
            <input type="number" name="profundidade" step="0.01" value="${isEdit ? product.profundidade || '10' : '10'}" required />
          </div>
          <div class="form-group">
            <label>Peso (kg)</label>
            <input type="number" name="peso" step="0.01" value="${isEdit ? product.peso || '1' : '1'}" required />
          </div>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">${isEdit ? 'Atualizar' : 'Criar'} Produto</button>
          <button type="button" class="btn btn-outline" onclick="hideProductForm()">Cancelar</button>
        </div>
      </form>
    </div>
  `;
}

// esconde o formulario de produto
export function hideProductForm() {
  document.getElementById("product-form-container").innerHTML = "";
}

// extrai dados do formulario de produto e converte campos numericos
function extractProductPayload(form) {
  const formData = new FormData(form);
  const data = Object.fromEntries(formData);
  const categoriaIds = formData.getAll('categorias').map(Number);

  const payload = {
    nome: data.nome,
    marca: data.marca,
    descricao: data.descricao,
    imagemUrl: data.imagemUrl || null,
    altura: Number(data.altura),
    largura: Number(data.largura),
    profundidade: Number(data.profundidade),
    peso: Number(data.peso),
    valorCusto: Number(data.valorCusto),
    valorVenda: Number(data.valorVenda),
    codigoBarras: data.codigoBarras,
    categorias: categoriaIds.map(id => ({ id })),
    grupoPrecificacao: { id: Number(data.grupoPrecificacaoId) },
  };

  return payload;
}

// handler para criar produto - converte campos numericos e envia para API
export async function handleCreateProduct(e, renderFn) {
  e.preventDefault();
  const payload = extractProductPayload(e.target);

  try {
    await apiRequest("/api/produtos", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    showToast("Produto criado com sucesso!");
    await loadProducts();
    hideProductForm();
    if (renderFn) renderFn();
    else document.getElementById("admin-content").innerHTML = AdminProductsTab();
  } catch (err) {
    showToast("Erro ao criar produto: " + err.message, "error");
  }
}

// handler para atualizar produto - RF0014
export async function handleUpdateProduct(e, productId) {
  e.preventDefault();
  const payload = extractProductPayload(e.target);

  try {
    await apiRequest(`/api/produtos/${productId}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
    showToast("Produto atualizado com sucesso!");
    await loadProducts();
    hideProductForm();
    document.getElementById("admin-content").innerHTML = AdminProductsTab();
  } catch (err) {
    showToast("Erro ao atualizar produto: " + err.message, "error");
  }
}

// abre o formulario de edicao de produto
export function editProduct(id) {
  showProductForm(id);
}

// exclui um produto apos confirmacao do usuario
export async function deleteProduct(id, renderFn) {
  if (!confirm("Tem certeza que deseja excluir este produto?")) return;

  try {
    await apiRequest(`/api/produtos/${id}`, { method: "DELETE" });
    showToast("Produto excluído com sucesso!");
    await loadProducts();
    renderFn();
  } catch (err) {
    showToast("Erro ao excluir produto: " + err.message, "error");
  }
}

// exibe o formulario de criar/editar usuario (admin)
export function showUserForm(userId = null) {
  const user = userId ? state.users.find(u => u.id === userId) : null;
  const isEdit = !!user;

  const container = document.getElementById("user-form-container");
  container.innerHTML = `
    <div class="admin-form">
      <h3>${isEdit ? 'Editar Usuário' : 'Novo Usuário'}</h3>
      <form onsubmit="${isEdit ? 'handleUpdateUser' : 'handleCreateUser'}(event)">
        ${isEdit ? `<input type="hidden" name="id" value="${user.id}" />` : ''}
        <div class="form-row">
          <div class="form-group">
            <label>Nome *</label>
            <input type="text" name="nome" value="${isEdit ? user.nome : ''}" required />
          </div>
          <div class="form-group">
            <label>Email *</label>
            <input type="email" name="email" value="${isEdit ? user.email : ''}" required ${isEdit ? 'readonly' : ''} />
          </div>
        </div>
        ${!isEdit ? `
        <div class="form-group">
          <label>Senha *</label>
          <input type="password" name="senha" required
                 oninput="validarSenhaTempoReal(this.value)" />
          <div class="password-requirements">
            <div class="pwd-req invalid" data-req="length">
              <i class="fas fa-check-circle"></i> Mínimo 8 caracteres
            </div>
            <div class="pwd-req invalid" data-req="uppercase">
              <i class="fas fa-check-circle"></i> Uma letra maiúscula
            </div>
            <div class="pwd-req invalid" data-req="lowercase">
              <i class="fas fa-check-circle"></i> Uma letra minúscula
            </div>
            <div class="pwd-req invalid" data-req="special">
              <i class="fas fa-check-circle"></i> Um caractere especial
            </div>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Confirmar Senha *</label>
            <input type="password" name="confirmacaoSenha" required />
          </div>
          <div class="form-group">
            <label>Role *</label>
            <select name="role" required>
              <option value="CLIENTE">Cliente</option>
              <option value="ADMIN">Administrador</option>
            </select>
          </div>
        </div>
        ` : ''}
        <div class="form-row">
          <div class="form-group">
            <label>CPF</label>
            <input type="text" name="cpf" value="${isEdit && user.cpf ? user.cpf : ''}" maxlength="14"
                   oninput="mascaraCpf(this)" />
          </div>
          <div class="form-group">
            <label>Telefone</label>
            <input type="tel" name="telefone" value="${isEdit && user.telefone ? user.telefone : ''}" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Gênero</label>
            <select name="genero">
              <option value="">Não informar</option>
              <option value="MASCULINO" ${isEdit && user.genero === 'MASCULINO' ? 'selected' : ''}>Masculino</option>
              <option value="FEMININO" ${isEdit && user.genero === 'FEMININO' ? 'selected' : ''}>Feminino</option>
            </select>
          </div>
          <div class="form-group">
            <label>Data de Nascimento</label>
            <input type="date" name="dataNascimento" value="${isEdit && user.dataNascimento ? user.dataNascimento : ''}" />
          </div>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">${isEdit ? 'Atualizar' : 'Criar'} Usuário</button>
          <button type="button" class="btn btn-outline" onclick="hideUserForm()">Cancelar</button>
        </div>
      </form>
    </div>
  `;
}

// esconde o formulario de usuario
export function hideUserForm() {
  document.getElementById("user-form-container").innerHTML = "";
}

// handler para criar usuario pelo admin - valida senha e envia para API
export async function handleCreateUser(e) {
  e.preventDefault();
  const form = e.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData);

  // RNF0031 - valida senha forte
  const senhaError = validarSenhaForte(data.senha);
  if (senhaError) {
    showFieldError('senha', senhaError);
    showToast(senhaError, "error");
    return;
  }

  // RNF0032 - valida confirmacao de senha
  const confirmError = validarConfirmacaoSenha(data.senha, data.confirmacaoSenha);
  if (confirmError) {
    showFieldError('confirmacaoSenha', confirmError);
    showToast(confirmError, "error");
    return;
  }

  // valida CPF se preenchido
  const cpfError = validarCpf(data.cpf);
  if (cpfError) {
    showFieldError('cpf', cpfError);
    showToast(cpfError, "error");
    return;
  }

  delete data.confirmacaoSenha;

  // remove campos vazios
  Object.keys(data).forEach((key) => {
    if (data[key] === "") delete data[key];
  });

  try {
    await apiRequest("/api/usuarios", {
      method: "POST",
      body: JSON.stringify(data),
    });
    showToast("Usuário criado com sucesso!");
    await loadUsers();
    hideUserForm();
    document.getElementById("admin-content").innerHTML = AdminUsersTab();
  } catch (err) {
    showToast("Erro ao criar usuário: " + err.message, "error");
  }
}

// handler para atualizar usuario existente pelo admin
export async function handleUpdateUser(e) {
  e.preventDefault();
  const form = e.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData);
  const id = data.id;
  delete data.id;

  // valida CPF se preenchido
  const cpfError = validarCpf(data.cpf);
  if (cpfError) {
    showFieldError('cpf', cpfError);
    showToast(cpfError, "error");
    return;
  }

  // remove campos vazios
  Object.keys(data).forEach((key) => {
    if (data[key] === "") delete data[key];
  });

  try {
    await apiRequest(`/api/usuarios/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    });
    showToast("Usuário atualizado com sucesso!");
    await loadUsers();
    hideUserForm();
    document.getElementById("admin-content").innerHTML = AdminUsersTab();
  } catch (err) {
    showToast("Erro ao atualizar usuário: " + err.message, "error");
  }
}

// abre o formulario de edicao de usuario
export function editUser(id) {
  showUserForm(id);
}

// exclui um usuario apos confirmacao
export async function deleteUser(id) {
  if (!confirm("Tem certeza que deseja excluir este usuário?")) return;

  try {
    await apiRequest(`/api/usuarios/${id}`, { method: "DELETE" });
    showToast("Usuário excluído com sucesso!");
    await loadUsers();
    document.getElementById("admin-content").innerHTML = AdminUsersTab();
  } catch (err) {
    showToast("Erro ao excluir usuário: " + err.message, "error");
  }
}

// exibe o formulario para registrar movimentacao de estoque (admin)
export function showMovementForm() {
  const container = document.getElementById("movement-form-container");
  // cria opcoes de produtos para o select
  const options = state.products
    .map((p) => `<option value="${p.id}">${p.nome}</option>`)
    .join("");

  container.innerHTML = `
    <div class="admin-form">
      <h3>Nova Movimentação</h3>
      <form onsubmit="handleCreateMovement(event)">
        <div class="form-row">
          <div class="form-group">
            <label>Produto</label>
            <select name="produtoId" required>
              <option value="">Selecione</option>
              ${options}
            </select>
          </div>
          <div class="form-group">
            <label>Tipo</label>
            <select name="tipo" required>
              <option value="ENTRADA">ENTRADA</option>
              <option value="SAIDA">SAÍDA</option>
            </select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Quantidade</label>
            <input type="number" name="quantidade" min="1" required />
          </div>
          <div class="form-group">
            <label>Valor Custo (R$)</label>
            <input type="number" name="valorCusto" step="0.01" />
          </div>
        </div>
        <div class="form-group">
          <label>Fornecedor</label>
          <input type="text" name="fornecedor" />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">Salvar</button>
          <button type="button" class="btn btn-outline" onclick="hideMovementForm()">Cancelar</button>
        </div>
      </form>
    </div>
  `;
}

// esconde o formulario de movimentacao
export function hideMovementForm() {
  document.getElementById("movement-form-container").innerHTML = "";
}

// handler para criar movimentacao de estoque - envia para API
export async function handleCreateMovement(e) {
  e.preventDefault();
  const form = e.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData);

  const payload = {
    produto: { id: Number(data.produtoId) },
    tipo: data.tipo,
    quantidade: Number(data.quantidade),
  };

  if (data.valorCusto) payload.valorCusto = Number(data.valorCusto);
  if (data.fornecedor) payload.fornecedor = data.fornecedor;

  try {
    await apiRequest("/api/movimentacoes", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    showToast("Movimentação registrada com sucesso!");
    await loadStock();
    await loadMovements();
    hideMovementForm();
    document.getElementById("admin-content").innerHTML = AdminMovementsTab();
  } catch (err) {
    showToast("Erro ao registrar movimentação: " + err.message, "error");
  }
}

// --- INATIVACAO/ATIVACAO DE PRODUTO - RF0012, RF0016, RN0015, RN0017 ---

// exibe form de inativacao de produto (com justificativa e categoria)
export function showInactivateProductForm(productId) {
  const container = document.getElementById("product-form-container");
  container.innerHTML = `
    <div class="admin-form">
      <h3>Inativar Produto #${productId}</h3>
      <form onsubmit="handleInactivateProduct(event, ${productId})">
        <div class="form-group">
          <label>Categoria de Inativacao *</label>
          <select name="categoriaInativacao" required>
            <option value="">Selecione</option>
            <option value="FORA_DE_MERCADO">Fora de Mercado</option>
            <option value="SEM_ESTOQUE">Sem Estoque</option>
            <option value="DESCONTINUADO">Descontinuado</option>
            <option value="DEFEITO">Defeito</option>
            <option value="OUTRO">Outro</option>
          </select>
        </div>
        <div class="form-group">
          <label>Justificativa *</label>
          <textarea name="justificativa" rows="3" required></textarea>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-danger">Inativar Produto</button>
          <button type="button" class="btn btn-outline" onclick="hideProductForm()">Cancelar</button>
        </div>
      </form>
    </div>
  `;
}

// exibe form de ativacao de produto
export function showActivateProductForm(productId) {
  const container = document.getElementById("product-form-container");
  container.innerHTML = `
    <div class="admin-form">
      <h3>Ativar Produto #${productId}</h3>
      <form onsubmit="handleActivateProduct(event, ${productId})">
        <div class="form-group">
          <label>Categoria de Ativacao *</label>
          <select name="categoriaAtivacao" required>
            <option value="">Selecione</option>
            <option value="REPOSICAO_ESTOQUE">Reposicao de Estoque</option>
            <option value="RETORNO_MERCADO">Retorno ao Mercado</option>
            <option value="CORRECAO">Correcao</option>
            <option value="OUTRO">Outro</option>
          </select>
        </div>
        <div class="form-group">
          <label>Justificativa *</label>
          <textarea name="justificativa" rows="3" required></textarea>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">Ativar Produto</button>
          <button type="button" class="btn btn-outline" onclick="hideProductForm()">Cancelar</button>
        </div>
      </form>
    </div>
  `;
}

// handler para inativar produto
export async function handleInactivateProduct(e, productId) {
  e.preventDefault();
  const formData = new FormData(e.target);
  const data = Object.fromEntries(formData);
  try {
    await apiRequest(`/api/produtos/${productId}/inativar`, {
      method: "PUT",
      body: JSON.stringify({
        motivo: data.justificativa,
        categoriaInativacao: data.categoriaInativacao,
        justificativa: data.justificativa,
      }),
    });
    showToast("Produto inativado com sucesso!");
    await loadProducts();
    hideProductForm();
    document.getElementById("admin-content").innerHTML = AdminProductsTab();
  } catch (err) {
    showToast("Erro ao inativar produto: " + err.message, "error");
  }
}

// handler para ativar produto
export async function handleActivateProduct(e, productId) {
  e.preventDefault();
  const formData = new FormData(e.target);
  const data = Object.fromEntries(formData);
  try {
    await apiRequest(`/api/produtos/${productId}/ativar`, {
      method: "PUT",
      body: JSON.stringify({
        motivo: data.justificativa,
        categoriaAtivacao: data.categoriaAtivacao,
        justificativa: data.justificativa,
      }),
    });
    showToast("Produto ativado com sucesso!");
    await loadProducts();
    hideProductForm();
    document.getElementById("admin-content").innerHTML = AdminProductsTab();
  } catch (err) {
    showToast("Erro ao ativar produto: " + err.message, "error");
  }
}

// --- ATIVAR/INATIVAR USUARIO - RF0023 ---

// alterna status ativo/inativo do usuario
export async function toggleUserStatus(userId, activate) {
  const action = activate ? 'ativar' : 'inativar';
  if (!confirm(`Tem certeza que deseja ${action} este usuario?`)) return;
  try {
    await apiRequest(`/api/usuarios/${userId}/${action}`, { method: "PUT" });
    showToast(`Usuario ${activate ? 'ativado' : 'inativado'} com sucesso!`);
    await loadUsers();
    document.getElementById("admin-content").innerHTML = AdminUsersTab();
  } catch (err) {
    showToast(`Erro ao ${action} usuario: ` + err.message, "error");
  }
}

// filtra usuarios na tabela - RF0024
export function filterUsers() {
  const input = document.getElementById("user-search-input");
  const query = input.value.toLowerCase();
  const filtered = state.users.filter(
    u => u.nome.toLowerCase().includes(query) ||
         u.email.toLowerCase().includes(query) ||
         (u.cpf && u.cpf.includes(query))
  );
  document.getElementById("users-table-container").innerHTML = renderUsersTable(filtered);
}

// --- ABA PEDIDOS (ADMIN) - RF0038, RF0039, RN0038 ---

// carrega todos os pedidos para o admin
async function loadAllOrders() {
  try {
    const data = await apiRequest("/api/compras");
    state.adminOrders = Array.isArray(data) ? data : (data.content || []);
  } catch (err) {
    console.error("Erro ao carregar pedidos:", err);
    state.adminOrders = [];
  }
}

// aba de pedidos do admin
export function AdminOrdersTab() {
  const orders = state.adminOrders || [];
  return `
    <div class="admin-section">
      <div class="admin-header">
        <h2>Gerenciar Pedidos</h2>
      </div>

      <div class="products-filters" style="margin-bottom: 1rem;">
        <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
          <button class="btn btn-sm ${!state.adminOrderFilter ? 'btn-primary' : 'btn-outline'}" onclick="filterAdminOrders('')">Todos</button>
          <button class="btn btn-sm ${state.adminOrderFilter === 'EM_PROCESSAMENTO' ? 'btn-primary' : 'btn-outline'}" onclick="filterAdminOrders('EM_PROCESSAMENTO')">Em Processamento</button>
          <button class="btn btn-sm ${state.adminOrderFilter === 'APROVADA' ? 'btn-primary' : 'btn-outline'}" onclick="filterAdminOrders('APROVADA')">Aprovadas</button>
          <button class="btn btn-sm ${state.adminOrderFilter === 'EM_TRANSITO' ? 'btn-primary' : 'btn-outline'}" onclick="filterAdminOrders('EM_TRANSITO')">Em Transito</button>
          <button class="btn btn-sm ${state.adminOrderFilter === 'ENTREGUE' ? 'btn-primary' : 'btn-outline'}" onclick="filterAdminOrders('ENTREGUE')">Entregues</button>
          <button class="btn btn-sm ${state.adminOrderFilter === 'REPROVADA' ? 'btn-primary' : 'btn-outline'}" onclick="filterAdminOrders('REPROVADA')">Reprovadas</button>
        </div>
      </div>

      <div class="admin-table" id="admin-orders-table">
        ${renderAdminOrdersTable(orders)}
      </div>
    </div>
  `;
}

function renderAdminOrdersTable(orders) {
  const filter = state.adminOrderFilter;
  const sorted = orders.slice().sort((a, b) => b.id - a.id);
  const filtered = filter ? sorted.filter(o => o.statusCompra === filter) : sorted;

  return `
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>Data</th>
          <th>Cliente</th>
          <th>Itens</th>
          <th>Valor</th>
          <th>Pagamento</th>
          <th>Status</th>
          <th>Acoes</th>
        </tr>
      </thead>
      <tbody>
        ${filtered.length > 0
          ? filtered.map(o => {
              const info = STATUS_MAP[o.statusCompra] || { label: o.statusCompra, class: 'status-default' };
              const itens = o.itens || [];
              const qtdItens = itens.reduce((sum, i) => sum + (i.quantidade || 0), 0);
              const pagamentos = o.pagamentos || [];
              const formasPagamento = [...new Set(pagamentos.map(p => {
                if (p.formaPagamento === 'CARTAO_CREDITO') return 'Cartao';
                if (p.formaPagamento === 'CUPOM_PROMOCIONAL') return 'Cupom Promo';
                if (p.formaPagamento === 'CUPOM_TROCA') return 'Cupom Troca';
                return p.formaPagamento || '-';
              }))];
              const clienteNome = o.cliente?.nome || o.cliente?.usuario?.nome || '-';
              return `
                <tr class="admin-order-row" onclick="toggleAdminOrderDetails(${o.id})" style="cursor: pointer;" title="Clique para ver detalhes">
                  <td>#${o.id}</td>
                  <td>${o.dataCompra ? new Date(o.dataCompra).toLocaleDateString() : '-'}</td>
                  <td>${clienteNome}</td>
                  <td>${qtdItens} ${qtdItens === 1 ? 'item' : 'itens'}</td>
                  <td>R$ ${o.valorTotal != null ? Number(o.valorTotal).toFixed(2) : '0.00'}</td>
                  <td>${formasPagamento.length > 0 ? formasPagamento.join(' + ') : '-'}</td>
                  <td><span class="badge-status ${info.class}">${info.label}</span></td>
                  <td onclick="event.stopPropagation()">${renderOrderActions(o)}</td>
                </tr>
                <tr id="admin-order-details-${o.id}" style="display: none;">
                  <td colspan="8" style="padding: 0;">
                    ${renderAdminOrderDetails(o)}
                  </td>
                </tr>
              `;
            }).join('')
          : '<tr><td colspan="8" class="text-center">Nenhum pedido encontrado</td></tr>'
        }
      </tbody>
    </table>
  `;
}

function renderAdminOrderDetails(order) {
  const itens = order.itens || [];
  const pagamentos = order.pagamentos || [];
  const endereco = order.enderecoEntrega;
  const cliente = order.cliente;

  return `
    <div class="admin-order-detail-panel">
      <div class="admin-order-detail-grid">
        <div class="admin-order-detail-section">
          <h4><i class="fas fa-user"></i> Cliente</h4>
          <p><strong>Nome:</strong> ${cliente?.nome || '-'}</p>
          <p><strong>Email:</strong> ${cliente?.email || cliente?.usuario?.email || '-'}</p>
          <p><strong>CPF:</strong> ${cliente?.cpf || '-'}</p>
          <p><strong>Telefone:</strong> ${cliente?.telefone || '-'}</p>
        </div>

        <div class="admin-order-detail-section">
          <h4><i class="fas fa-map-marker-alt"></i> Endereco de Entrega</h4>
          ${endereco ? `
            <p>${endereco.logradouro || ''}, ${endereco.numero || ''}</p>
            <p>${endereco.bairro || ''} - ${endereco.cidade || ''}/${endereco.estado || ''}</p>
            <p>CEP: ${endereco.cep || '-'}</p>
            ${endereco.complemento ? `<p>Complemento: ${endereco.complemento}</p>` : ''}
          ` : '<p>-</p>'}
        </div>

        <div class="admin-order-detail-section">
          <h4><i class="fas fa-credit-card"></i> Pagamentos</h4>
          ${pagamentos.length > 0 ? pagamentos.map(p => {
            let forma = p.formaPagamento === 'CARTAO_CREDITO' ? 'Cartao de Credito'
              : p.formaPagamento === 'CUPOM_PROMOCIONAL' ? 'Cupom Promocional'
              : p.formaPagamento === 'CUPOM_TROCA' ? 'Cupom de Troca'
              : p.formaPagamento;
            let detalhe = '';
            if (p.cartaoCredito) {
              detalhe = ` (****${p.cartaoCredito.numero ? p.cartaoCredito.numero.slice(-4) : '****'})`;
            }
            if (p.cupom) {
              detalhe = ` (${p.cupom.codigo || ''})`;
            }
            return `<p>${forma}${detalhe}: <strong>R$ ${Number(p.valor).toFixed(2)}</strong></p>`;
          }).join('') : '<p>-</p>'}
        </div>
      </div>

      <div class="admin-order-detail-section">
        <h4><i class="fas fa-box"></i> Itens do Pedido</h4>
        <table class="admin-order-items-table">
          <thead>
            <tr>
              <th>Produto</th>
              <th>Qtd</th>
              <th>Preco Unit.</th>
              <th>Subtotal</th>
            </tr>
          </thead>
          <tbody>
            ${itens.map(item => `
              <tr>
                <td>${item.produto ? item.produto.nome : '-'}</td>
                <td>${item.quantidade}</td>
                <td>R$ ${Number(item.precoUnitario).toFixed(2)}</td>
                <td>R$ ${(item.quantidade * item.precoUnitario).toFixed(2)}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
        <div style="margin-top: 0.75rem; text-align: right; font-size: 0.9rem;">
          ${order.frete != null ? `<p>Frete: <strong>${order.frete === 0 ? 'Gratis (SP)' : 'R$ ' + Number(order.frete).toFixed(2)}</strong></p>` : ''}
          <p>Total: <strong>R$ ${order.valorTotal != null ? Number(order.valorTotal).toFixed(2) : '0.00'}</strong></p>
        </div>
      </div>
    </div>
  `;
}

function renderOrderActions(order) {
  let actions = '';
  if (order.statusCompra === 'EM_PROCESSAMENTO') {
    actions += `
      <button class="btn btn-sm btn-primary" onclick="updateOrderStatus(${order.id}, 'APROVADA')" title="Aprovar">
        <i class="fas fa-check"></i> Aprovar
      </button>
      <button class="btn btn-sm btn-danger" onclick="updateOrderStatus(${order.id}, 'REPROVADA')" title="Reprovar">
        <i class="fas fa-times"></i> Reprovar
      </button>
    `;
  } else if (order.statusCompra === 'APROVADA') {
    actions += `
      <button class="btn btn-sm btn-primary" onclick="updateOrderStatus(${order.id}, 'EM_TRANSITO')" title="Despachar">
        <i class="fas fa-truck"></i> Despachar
      </button>
    `;
  } else if (order.statusCompra === 'EM_TRANSITO') {
    actions += `
      <button class="btn btn-sm btn-primary" onclick="updateOrderStatus(${order.id}, 'ENTREGUE')" title="Confirmar Entrega">
        <i class="fas fa-box"></i> Confirmar Entrega
      </button>
    `;
  }
  return actions || '-';
}

// atualiza status de um pedido - RF0038, RF0039, RN0038
export async function updateOrderStatus(orderId, newStatus) {
  const labels = { APROVADA: 'aprovar', REPROVADA: 'reprovar', EM_TRANSITO: 'despachar', ENTREGUE: 'confirmar entrega' };
  if (!confirm(`Tem certeza que deseja ${labels[newStatus] || 'alterar'} este pedido?`)) return;

  try {
    await apiRequest(`/api/compras/${orderId}/status`, {
      method: "PUT",
      body: JSON.stringify({ status: newStatus }),
    });
    showToast(`Pedido ${labels[newStatus] || 'atualizado'} com sucesso!`);
    await loadAllOrders();
    document.getElementById("admin-content").innerHTML = AdminOrdersTab();
  } catch (err) {
    showToast("Erro ao atualizar pedido: " + err.message, "error");
  }
}

// alterna visibilidade dos detalhes de um pedido no admin
export function toggleAdminOrderDetails(orderId) {
  const row = document.getElementById(`admin-order-details-${orderId}`);
  if (!row) return;
  const isHidden = row.style.display === 'none';
  row.style.display = isHidden ? 'table-row' : 'none';
}

// filtra pedidos do admin por status
export function filterAdminOrders(status) {
  state.adminOrderFilter = status;
  document.getElementById("admin-content").innerHTML = AdminOrdersTab();
}

// --- ABA TROCAS (ADMIN) - RF0041, RF0042, RF0043 ---

// carrega todas as trocas
async function loadAllTrocas() {
  try {
    const data = await apiRequest("/api/trocas");
    state.adminTrocas = Array.isArray(data) ? data : (data.content || []);
  } catch (err) {
    console.error("Erro ao carregar trocas:", err);
    state.adminTrocas = [];
  }
}

// mapeamento de status de troca
const TROCA_STATUS_MAP = {
  EM_TROCA: { label: 'Em Troca', class: 'status-exchange' },
  TROCA_AUTORIZADA: { label: 'Autorizada', class: 'status-exchange-auth' },
  TROCA_RECUSADA: { label: 'Recusada', class: 'status-rejected' },
  TROCADA: { label: 'Trocada', class: 'status-exchanged' },
};

// aba de trocas do admin
export function AdminTrocasTab() {
  const trocas = state.adminTrocas || [];
  return `
    <div class="admin-section">
      <div class="admin-header">
        <h2>Gerenciar Trocas</h2>
      </div>

      <div class="products-filters" style="margin-bottom: 1rem;">
        <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
          <button class="btn btn-sm ${!state.adminTrocaFilter ? 'btn-primary' : 'btn-outline'}" onclick="filterAdminTrocas('')">Todas</button>
          <button class="btn btn-sm ${state.adminTrocaFilter === 'EM_TROCA' ? 'btn-primary' : 'btn-outline'}" onclick="filterAdminTrocas('EM_TROCA')">Em Troca</button>
          <button class="btn btn-sm ${state.adminTrocaFilter === 'TROCA_AUTORIZADA' ? 'btn-primary' : 'btn-outline'}" onclick="filterAdminTrocas('TROCA_AUTORIZADA')">Autorizadas</button>
          <button class="btn btn-sm ${state.adminTrocaFilter === 'TROCADA' ? 'btn-primary' : 'btn-outline'}" onclick="filterAdminTrocas('TROCADA')">Trocadas</button>
        </div>
      </div>

      <div class="admin-table">
        ${renderAdminTrocasTable(trocas)}
      </div>
    </div>
  `;
}

function renderAdminTrocasTable(trocas) {
  const filter = state.adminTrocaFilter;
  const sorted = trocas.slice().sort((a, b) => b.id - a.id);
  const filtered = filter ? sorted.filter(t => t.statusTroca === filter) : sorted;

  return `
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>Pedido</th>
          <th>Cliente</th>
          <th>Data</th>
          <th>Itens</th>
          <th>Motivo</th>
          <th>Status</th>
          <th>Acoes</th>
        </tr>
      </thead>
      <tbody>
        ${filtered.length > 0
          ? filtered.map(t => {
              const info = TROCA_STATUS_MAP[t.statusTroca] || { label: t.statusTroca, class: 'status-default' };
              const itens = t.itens || [];
              const qtdItens = itens.reduce((sum, i) => sum + (i.quantidade || 0), 0);
              const clienteNome = t.compra?.cliente?.nome || '-';
              return `
                <tr class="admin-order-row" onclick="toggleAdminTrocaDetails(${t.id})" style="cursor: pointer;" title="Clique para ver detalhes">
                  <td>#${t.id}</td>
                  <td>#${t.compraId || t.compra?.id || '-'}</td>
                  <td>${clienteNome}</td>
                  <td>${t.dataSolicitacao ? new Date(t.dataSolicitacao).toLocaleDateString() : '-'}</td>
                  <td>${qtdItens} ${qtdItens === 1 ? 'item' : 'itens'}</td>
                  <td title="${t.motivo || ''}">${t.motivo ? (t.motivo.length > 30 ? t.motivo.substring(0, 30) + '...' : t.motivo) : '-'}</td>
                  <td><span class="badge-status ${info.class}">${info.label}</span></td>
                  <td onclick="event.stopPropagation()">${renderTrocaActions(t)}</td>
                </tr>
                <tr id="admin-troca-details-${t.id}" style="display: none;">
                  <td colspan="8" style="padding: 0;">
                    ${renderAdminTrocaDetails(t)}
                  </td>
                </tr>
              `;
            }).join('')
          : '<tr><td colspan="8" class="text-center">Nenhuma troca encontrada</td></tr>'
        }
      </tbody>
    </table>
  `;
}

function renderAdminTrocaDetails(troca) {
  const itens = troca.itens || [];
  const cliente = troca.compra?.cliente;
  const compra = troca.compra;

  return `
    <div class="admin-order-detail-panel">
      <div class="admin-order-detail-grid">
        <div class="admin-order-detail-section">
          <h4><i class="fas fa-user"></i> Cliente</h4>
          <p><strong>Nome:</strong> ${cliente?.nome || '-'}</p>
          <p><strong>Email:</strong> ${cliente?.email || cliente?.usuario?.email || '-'}</p>
          <p><strong>CPF:</strong> ${cliente?.cpf || '-'}</p>
        </div>

        <div class="admin-order-detail-section">
          <h4><i class="fas fa-shopping-bag"></i> Pedido Original #${compra?.id || '-'}</h4>
          <p><strong>Data da Compra:</strong> ${compra?.dataCompra ? new Date(compra.dataCompra).toLocaleDateString() : '-'}</p>
          <p><strong>Valor Total:</strong> R$ ${compra?.valorTotal != null ? Number(compra.valorTotal).toFixed(2) : '-'}</p>
          <p><strong>Status do Pedido:</strong> ${(STATUS_MAP[compra?.statusCompra] || {}).label || compra?.statusCompra || '-'}</p>
        </div>

        <div class="admin-order-detail-section">
          <h4><i class="fas fa-info-circle"></i> Detalhes da Troca</h4>
          <p><strong>Solicitada em:</strong> ${troca.dataSolicitacao ? new Date(troca.dataSolicitacao).toLocaleDateString() : '-'}</p>
          ${troca.dataConclusao ? `<p><strong>Concluida em:</strong> ${new Date(troca.dataConclusao).toLocaleDateString()}</p>` : ''}
          <p><strong>Motivo:</strong> ${troca.motivo || 'Nao informado'}</p>
        </div>
      </div>

      <div class="admin-order-detail-section">
        <h4><i class="fas fa-exchange-alt"></i> Itens da Troca</h4>
        <table class="admin-order-items-table">
          <thead>
            <tr>
              <th>Produto</th>
              <th>Quantidade</th>
            </tr>
          </thead>
          <tbody>
            ${itens.length > 0 ? itens.map(item => `
              <tr>
                <td>${item.produto ? item.produto.nome : '-'}</td>
                <td>${item.quantidade}</td>
              </tr>
            `).join('') : '<tr><td colspan="2">Nenhum item registrado</td></tr>'}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// alterna visibilidade dos detalhes de uma troca no admin
export function toggleAdminTrocaDetails(trocaId) {
  const row = document.getElementById(`admin-troca-details-${trocaId}`);
  if (!row) return;
  row.style.display = row.style.display === 'none' ? 'table-row' : 'none';
}

// filtra trocas do admin por status
export function filterAdminTrocas(status) {
  state.adminTrocaFilter = status;
  document.getElementById("admin-content").innerHTML = AdminTrocasTab();
}

function renderTrocaActions(troca) {
  let actions = '';
  if (troca.statusTroca === 'EM_TROCA') {
    actions += `
      <button class="btn btn-sm btn-primary" onclick="authorizeTroca(${troca.id})" title="Autorizar">
        <i class="fas fa-check"></i> Autorizar
      </button>
      <button class="btn btn-sm btn-danger" onclick="denyTroca(${troca.id})" title="Negar">
        <i class="fas fa-times"></i> Negar
      </button>
    `;
  } else if (troca.statusTroca === 'TROCA_AUTORIZADA') {
    actions += `
      <button class="btn btn-sm btn-primary" onclick="confirmTrocaReceived(${troca.id})" title="Confirmar Recebimento">
        <i class="fas fa-box"></i> Recebido
      </button>
    `;
  }
  return actions || '-';
}

// autorizar troca - RF0041
export async function authorizeTroca(trocaId) {
  if (!confirm("Autorizar esta troca?")) return;
  try {
    await apiRequest(`/api/trocas/${trocaId}/autorizar`, { method: "PUT" });
    showToast("Troca autorizada com sucesso!");
    await loadAllTrocas();
    document.getElementById("admin-content").innerHTML = AdminTrocasTab();
  } catch (err) {
    showToast("Erro ao autorizar troca: " + err.message, "error");
  }
}

// negar troca - admin
export async function denyTroca(trocaId) {
  const motivo = prompt("Motivo da recusa:");
  if (motivo === null) return;
  try {
    await apiRequest(`/api/trocas/${trocaId}/negar`, {
      method: "PUT",
      body: JSON.stringify({ motivo }),
    });
    showToast("Troca negada.");
    await loadAllTrocas();
    document.getElementById("admin-content").innerHTML = AdminTrocasTab();
  } catch (err) {
    showToast("Erro ao negar troca: " + err.message, "error");
  }
}

// confirmar recebimento da troca - RF0043
export async function confirmTrocaReceived(trocaId) {
  if (!confirm("Confirmar recebimento desta troca?")) return;
  const reestocar = confirm("Os itens devem retornar ao estoque?");
  try {
    await apiRequest(`/api/trocas/${trocaId}/receber`, {
      method: "PUT",
      body: JSON.stringify({ reestocar }),
    });
    showToast(`Troca confirmada! Cupom gerado.${reestocar ? ' Estoque atualizado.' : ''}`);
    await loadAllTrocas();
    document.getElementById("admin-content").innerHTML = AdminTrocasTab();
  } catch (err) {
    showToast("Erro ao confirmar troca: " + err.message, "error");
  }
}

// --- ABA CUPONS (ADMIN) ---

let adminCupons = [];

async function loadAllCupons() {
  try {
    const data = await apiRequest("/api/cupons");
    adminCupons = Array.isArray(data) ? data : [];
  } catch (err) {
    console.error("Erro ao carregar cupons:", err);
    adminCupons = [];
  }
}

function AdminCuponsTab() {
  return `
    <div class="admin-section">
      <div class="admin-header">
        <h2>Gerenciar Cupons</h2>
        <button class="btn btn-primary btn-sm" onclick="showCupomForm()">
          <i class="fas fa-plus"></i> Novo Cupom Promocional
        </button>
      </div>

      <div id="cupom-form-container"></div>

      <div class="admin-table">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Codigo</th>
              <th>Tipo</th>
              <th>Valor</th>
              <th>Validade</th>
              <th>Cliente</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            ${adminCupons.length > 0
              ? adminCupons.map(c => `
                <tr>
                  <td>#${c.id}</td>
                  <td><strong>${c.codigo}</strong></td>
                  <td>${c.promocional ? 'Promocional' : 'Troca'}</td>
                  <td>R$ ${Number(c.valor).toFixed(2)}</td>
                  <td>${c.validade ? new Date(c.validade).toLocaleDateString() : '-'}</td>
                  <td>${c.cliente ? (c.cliente.nome || c.cliente.email || '-') : 'Global'}</td>
                  <td>
                    <span class="badge-status ${c.usado ? 'status-rejected' : 'status-active'}">
                      ${c.usado ? 'Utilizado' : 'Disponivel'}
                    </span>
                  </td>
                </tr>
              `).join('')
              : '<tr><td colspan="7" class="text-center">Nenhum cupom encontrado</td></tr>'
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

export function showCupomForm() {
  const container = document.getElementById('cupom-form-container');
  container.innerHTML = `
    <div class="admin-form">
      <h3>Novo Cupom Promocional</h3>
      <form onsubmit="handleCreateCupom(event)">
        <div class="form-group">
          <label>Codigo</label>
          <input type="text" name="codigo" placeholder="Ex: PROMO10" required class="input" style="text-transform: uppercase;" />
        </div>
        <div class="form-group">
          <label>Valor (R$)</label>
          <input type="number" name="valor" step="0.01" min="0.01" required class="input" />
        </div>
        <div class="form-group">
          <label>Validade (dias)</label>
          <input type="number" name="validadeDias" value="30" min="1" required class="input" />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">Criar Cupom</button>
          <button type="button" class="btn btn-outline" onclick="hideCupomForm()">Cancelar</button>
        </div>
      </form>
    </div>
  `;
}

export function hideCupomForm() {
  document.getElementById('cupom-form-container').innerHTML = '';
}

export async function handleCreateCupom(e) {
  e.preventDefault();
  const formData = new FormData(e.target);
  const data = Object.fromEntries(formData);

  try {
    await apiRequest("/api/cupons", {
      method: "POST",
      body: JSON.stringify({
        codigo: data.codigo,
        valor: Number(data.valor),
        validadeDias: Number(data.validadeDias),
      }),
    });
    showToast("Cupom promocional criado com sucesso!");
    hideCupomForm();
    await loadAllCupons();
    document.getElementById("admin-content").innerHTML = AdminCuponsTab();
  } catch (err) {
    showToast("Erro ao criar cupom: " + err.message, "error");
  }
}

// --- ABA AUDITORIA (ADMIN) - RNF0012 ---

// carrega logs de auditoria
async function loadAuditoria() {
  try {
    const data = await apiRequest("/api/auditoria");
    state.auditoriaLogs = Array.isArray(data) ? data : (data.content || []);
  } catch (err) {
    console.error("Erro ao carregar auditoria:", err);
    state.auditoriaLogs = [];
  }
}

// aba de auditoria do admin
export function AdminAuditoriaTab() {
  const logs = state.auditoriaLogs || [];
  return `
    <div class="admin-section">
      <div class="admin-header">
        <h2>Logs de Auditoria</h2>
      </div>

      <div class="products-filters" style="margin-bottom: 1rem;">
        <form onsubmit="handleFilterAuditoria(event)" style="display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: flex-end;">
          <div class="form-group" style="margin-bottom: 0;">
            <label style="font-size: 0.75rem;">Entidade</label>
            <input type="text" id="audit-entity" placeholder="Ex: Produto" style="padding: 0.5rem; border: 2px solid var(--gray-light); border-radius: var(--radius-sm); font-size: 0.813rem;" />
          </div>
          <div class="form-group" style="margin-bottom: 0;">
            <label style="font-size: 0.75rem;">Data Inicio</label>
            <input type="date" id="audit-start" style="padding: 0.5rem; border: 2px solid var(--gray-light); border-radius: var(--radius-sm); font-size: 0.813rem;" />
          </div>
          <div class="form-group" style="margin-bottom: 0;">
            <label style="font-size: 0.75rem;">Data Fim</label>
            <input type="date" id="audit-end" style="padding: 0.5rem; border: 2px solid var(--gray-light); border-radius: var(--radius-sm); font-size: 0.813rem;" />
          </div>
          <button type="submit" class="btn btn-primary btn-sm">Filtrar</button>
        </form>
      </div>

      <div class="admin-table">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Data</th>
              <th>Usuario</th>
              <th>Operacao</th>
              <th>Entidade</th>
              <th>Detalhes</th>
            </tr>
          </thead>
          <tbody>
            ${logs.length > 0
              ? logs.map(l => `
                <tr>
                  <td>#${l.id}</td>
                  <td>${l.dataHora ? new Date(l.dataHora).toLocaleString() : '-'}</td>
                  <td>${l.nomeUsuario || l.usuario?.nome || '-'}</td>
                  <td><span class="badge-role admin">${l.tipoOperacao || l.operacao || '-'}</span></td>
                  <td>${l.entidade || '-'}</td>
                  <td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis;">${l.detalhes || '-'}</td>
                </tr>
              `).join('')
              : '<tr><td colspan="6" class="text-center">Nenhum log encontrado</td></tr>'
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// filtrar auditoria por entidade e periodo
export async function handleFilterAuditoria(e) {
  e.preventDefault();
  const entity = document.getElementById('audit-entity').value;
  const start = document.getElementById('audit-start').value;
  const end = document.getElementById('audit-end').value;

  let url = '/api/auditoria?';
  if (entity) url += `entidade=${entity}&`;
  if (start) url += `dataInicio=${start}&`;
  if (end) url += `dataFim=${end}&`;

  try {
    const data = await apiRequest(url.slice(0, -1));
    state.auditoriaLogs = Array.isArray(data) ? data : (data.content || []);
    document.getElementById("admin-content").innerHTML = AdminAuditoriaTab();
  } catch (err) {
    showToast("Erro ao filtrar auditoria: " + err.message, "error");
  }
}
