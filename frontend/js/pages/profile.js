import { state } from '../state.js';
import { apiRequest } from '../api.js';
import { showToast } from '../utils.js';
import { Header } from '../components/header.js';
import { Footer } from '../components/footer.js';
import { loadAddresses, loadCards } from '../data.js';
import { ensureUserId } from '../auth.js';
import { validarSenhaForte, validarConfirmacaoSenha, validarCpf, mascaraCpf, showFieldError, validarSenhaTempoReal } from '../validators.js';

// pagina "Meu Perfil" com abas para dados pessoais, enderecos, cartoes, cupons e senha
export function ProfilePage() {
  if (!state.isAuthenticated) {
    return `
      ${Header()}
      <main>
        <section class="page-section">
          <div class="container">
            <div class="empty-state">
              <i class="fas fa-lock"></i>
              <h3>Acesso Restrito</h3>
              <p>Faça login para acessar seu perfil</p>
              <button class="btn btn-primary" onclick="navigate('login')">Entrar</button>
            </div>
          </div>
        </section>
      </main>
      ${Footer()}
    `;
  }

  const activeTab = state.profileTab || 'dados';

  return `
    ${Header()}
    <main>
      <section class="page-section">
        <div class="container">
          <h1 class="page-title">Meu Perfil</h1>

          <div class="admin-tabs">
            <button class="tab-btn ${activeTab === 'dados' ? 'active' : ''}" onclick="switchProfileTab('dados', event)">
              <i class="fas fa-user"></i> Dados Pessoais
            </button>
            <button class="tab-btn ${activeTab === 'enderecos' ? 'active' : ''}" onclick="switchProfileTab('enderecos', event)">
              <i class="fas fa-map-marker-alt"></i> Enderecos
            </button>
            <button class="tab-btn ${activeTab === 'cartoes' ? 'active' : ''}" onclick="switchProfileTab('cartoes', event)">
              <i class="fas fa-credit-card"></i> Cartoes
            </button>
            <button class="tab-btn ${activeTab === 'cupons' ? 'active' : ''}" onclick="switchProfileTab('cupons', event)">
              <i class="fas fa-ticket-alt"></i> Cupons
            </button>
            <button class="tab-btn ${activeTab === 'senha' ? 'active' : ''}" onclick="switchProfileTab('senha', event)">
              <i class="fas fa-key"></i> Alterar Senha
            </button>
          </div>

          <div id="profile-content">
            ${renderProfileTab(activeTab)}
          </div>
        </div>
      </section>
    </main>
    ${Footer()}
  `;
}

// renderiza o conteudo da aba ativa do perfil
function renderProfileTab(tab) {
  switch (tab) {
    case 'dados': return ProfileDadosTab();
    case 'enderecos': return ProfileEnderecosTab();
    case 'cartoes': return ProfileCartoesTab();
    case 'cupons': return ProfileCuponsTab();
    case 'senha': return ProfileSenhaTab();
    default: return ProfileDadosTab();
  }
}

// aba de dados pessoais - RF0022
function ProfileDadosTab() {
  const u = state.profileData || {};
  return `
    <div class="admin-section">
      <div class="admin-header">
        <h2>Dados Pessoais</h2>
      </div>
      <div id="profile-dados-form">
        <form onsubmit="handleUpdateProfile(event)">
          <div class="form-row">
            <div class="form-group">
              <label>Nome *</label>
              <input type="text" name="nome" value="${u.nome || ''}" required />
            </div>
            <div class="form-group">
              <label>Email</label>
              <input type="email" name="email" value="${u.email || state.user.email}" readonly />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>CPF</label>
              <input type="text" name="cpf" value="${u.cpf || ''}" maxlength="14"
                     oninput="mascaraCpf(this)" />
            </div>
            <div class="form-group">
              <label>Telefone</label>
              <input type="tel" name="telefone" value="${u.telefone || ''}" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Genero</label>
              <select name="genero">
                <option value="">Nao informar</option>
                <option value="MASCULINO" ${u.genero === 'MASCULINO' ? 'selected' : ''}>Masculino</option>
                <option value="FEMININO" ${u.genero === 'FEMININO' ? 'selected' : ''}>Feminino</option>
              </select>
            </div>
            <div class="form-group">
              <label>Data de Nascimento</label>
              <input type="date" name="dataNascimento" value="${u.dataNascimento || ''}" />
            </div>
          </div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary">Salvar Alteracoes</button>
          </div>
        </form>
      </div>
    </div>
  `;
}

// aba de enderecos de entrega - RF0026, RF0034/RNF0034
function ProfileEnderecosTab() {
  return `
    <div class="admin-section">
      <div class="admin-header">
        <h2>Enderecos de Entrega</h2>
        <button class="btn btn-primary" onclick="showProfileAddressForm()">
          <i class="fas fa-plus"></i> Novo Endereco
        </button>
      </div>

      <div id="profile-address-form-container"></div>

      <div class="admin-table">
        <table>
          <thead>
            <tr>
              <th>Finalidade</th>
              <th>Nome</th>
              <th>Logradouro</th>
              <th>Cidade/UF</th>
              <th>CEP</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            ${state.addresses.length > 0
              ? state.addresses.map(a => {
                const tipoLabel = a.tipoEndereco === 'COBRANCA' ? 'Cobranca' : a.tipoEndereco === 'AMBOS' ? 'Ambos' : 'Entrega';
                return `
                <tr>
                  <td><span class="badge-role">${tipoLabel}</span></td>
                  <td>${a.nomeEndereco || '-'}</td>
                  <td>${a.tipoLogradouro || ''} ${a.logradouro || ''}, ${a.numero || ''}</td>
                  <td>${a.cidade || '-'}/${a.estado || '-'}</td>
                  <td>${a.cep || '-'}</td>
                  <td>
                    <button class="btn-icon" onclick="editProfileAddress(${a.id})" title="Editar">
                      <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn-icon text-danger" onclick="deleteProfileAddress(${a.id})" title="Excluir">
                      <i class="fas fa-trash"></i>
                    </button>
                  </td>
                </tr>`;
              }).join('')
              : '<tr><td colspan="6" class="text-center">Nenhum endereco cadastrado</td></tr>'
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// aba de cartoes de credito - RF0027
function ProfileCartoesTab() {
  return `
    <div class="admin-section">
      <div class="admin-header">
        <h2>Cartoes de Credito</h2>
        <button class="btn btn-primary" onclick="showProfileCardForm()">
          <i class="fas fa-plus"></i> Novo Cartao
        </button>
      </div>

      <div id="profile-card-form-container"></div>

      <div class="admin-table">
        <table>
          <thead>
            <tr>
              <th>Bandeira</th>
              <th>Numero</th>
              <th>Nome Impresso</th>
              <th>Preferencial</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            ${state.cards.length > 0
              ? state.cards.map(c => `
                <tr>
                  <td>${c.bandeira || '-'}</td>
                  <td>**** **** **** ${(c.numero || '').slice(-4)}</td>
                  <td>${c.nomeImpresso || '-'}</td>
                  <td>${c.preferencial ? '<i class="fas fa-star text-warning"></i> Sim' : 'Nao'}</td>
                  <td>
                    <button class="btn-icon" onclick="showEditCardForm(${c.id}, '${(c.nomeImpresso || '').replace(/'/g, "\\'")}', ${c.preferencial})" title="Editar">
                      <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn-icon text-danger" onclick="deleteProfileCard(${c.id})" title="Excluir">
                      <i class="fas fa-trash"></i>
                    </button>
                  </td>
                </tr>
              `).join('')
              : '<tr><td colspan="5" class="text-center">Nenhum cartao cadastrado</td></tr>'
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// aba de cupons do cliente
function ProfileCuponsTab() {
  const cupons = state.cupons || [];
  return `
    <div class="admin-section">
      <div class="admin-header">
        <h2>Meus Cupons</h2>
      </div>

      ${cupons.length > 0 ? `
        <div class="admin-table">
          <table>
            <thead>
              <tr>
                <th>Codigo</th>
                <th>Tipo</th>
                <th>Valor</th>
                <th>Validade</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              ${cupons.map(c => `
                <tr>
                  <td><strong>${c.codigo}</strong></td>
                  <td>${c.promocional ? 'Promocional' : 'Troca'}</td>
                  <td>R$ ${c.valor != null ? Number(c.valor).toFixed(2) : '0.00'}</td>
                  <td>${c.validade ? new Date(c.validade).toLocaleDateString() : '-'}</td>
                  <td>
                    <span class="badge-status ${c.usado ? 'status-rejected' : 'status-active'}">
                      ${c.usado ? 'Utilizado' : 'Disponivel'}
                    </span>
                  </td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      ` : `
        <div class="empty-state">
          <i class="fas fa-ticket-alt"></i>
          <h3>Nenhum cupom disponivel</h3>
          <p>Seus cupons de troca e promocionais aparecerao aqui</p>
        </div>
      `}
    </div>
  `;
}

// aba de alteracao de senha - RF0028
function ProfileSenhaTab() {
  return `
    <div class="admin-section">
      <div class="admin-header">
        <h2>Alterar Senha</h2>
      </div>
      <form onsubmit="handleChangePassword(event)">
        <div class="form-group">
          <label>Senha Atual *</label>
          <input type="password" name="senhaAtual" required />
        </div>
        <div class="form-group">
          <label>Nova Senha *</label>
          <input type="password" name="novaSenha" required
                 oninput="validarSenhaTempoReal(this.value)" />
          <div class="password-requirements">
            <div class="pwd-req invalid" data-req="length">
              <i class="fas fa-check-circle"></i> Minimo 8 caracteres
            </div>
            <div class="pwd-req invalid" data-req="uppercase">
              <i class="fas fa-check-circle"></i> Uma letra maiuscula
            </div>
            <div class="pwd-req invalid" data-req="lowercase">
              <i class="fas fa-check-circle"></i> Uma letra minuscula
            </div>
            <div class="pwd-req invalid" data-req="special">
              <i class="fas fa-check-circle"></i> Um caractere especial
            </div>
          </div>
        </div>
        <div class="form-group">
          <label>Confirmar Nova Senha *</label>
          <input type="password" name="confirmacaoSenha" required />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">Alterar Senha</button>
        </div>
      </form>
    </div>
  `;
}

// troca entre as abas do perfil
export async function switchProfileTab(tab, evt) {
  const buttons = document.querySelectorAll(".tab-btn");
  buttons.forEach(btn => btn.classList.remove("active"));
  evt.target.closest(".tab-btn").classList.add("active");

  state.profileTab = tab;
  const content = document.getElementById("profile-content");

  if (tab === 'enderecos') {
    content.innerHTML = '<div class="loading">Carregando enderecos...</div>';
    await loadAddresses();
    content.innerHTML = ProfileEnderecosTab();
  } else if (tab === 'cartoes') {
    content.innerHTML = '<div class="loading">Carregando cartoes...</div>';
    await loadCards();
    content.innerHTML = ProfileCartoesTab();
  } else if (tab === 'cupons') {
    content.innerHTML = '<div class="loading">Carregando cupons...</div>';
    await loadProfileCupons();
    content.innerHTML = ProfileCuponsTab();
  } else {
    content.innerHTML = renderProfileTab(tab);
  }
}

// carrega dados do perfil do usuario logado
export async function loadProfileData() {
  await ensureUserId();
  if (!state.user?.id) return;
  try {
    const data = await apiRequest(`/api/usuarios/${state.user.id}`);
    state.profileData = data;
  } catch (err) {
    console.error("Erro ao carregar perfil:", err);
    state.profileData = {};
  }
}

// carrega cupons do usuario logado
async function loadProfileCupons() {
  if (!state.user?.id) return;
  try {
    const data = await apiRequest(`/api/cupons/${state.user.id}`);
    state.cupons = data || [];
  } catch (err) {
    console.error("Erro ao carregar cupons:", err);
    state.cupons = [];
  }
}

// handler para atualizar dados pessoais - RF0022
export async function handleUpdateProfile(e) {
  e.preventDefault();
  const formData = new FormData(e.target);
  const data = Object.fromEntries(formData);
  delete data.email;

  // valida CPF se preenchido
  const cpfError = validarCpf(data.cpf);
  if (cpfError) {
    showFieldError('cpf', cpfError);
    showToast(cpfError, "error");
    return;
  }

  Object.keys(data).forEach(key => {
    if (data[key] === "") delete data[key];
  });

  try {
    await apiRequest(`/api/usuarios/${state.user.id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    });
    showToast("Perfil atualizado com sucesso!");
    await loadProfileData();
    document.getElementById("profile-content").innerHTML = ProfileDadosTab();
  } catch (err) {
    showToast("Erro ao atualizar perfil: " + err.message, "error");
  }
}

// handler para alterar senha - RF0028
export async function handleChangePassword(e) {
  e.preventDefault();
  const formData = new FormData(e.target);
  const data = Object.fromEntries(formData);

  const senhaError = validarSenhaForte(data.novaSenha);
  if (senhaError) {
    showFieldError('novaSenha', senhaError);
    showToast(senhaError, "error");
    return;
  }

  const confirmError = validarConfirmacaoSenha(data.novaSenha, data.confirmacaoSenha);
  if (confirmError) {
    showFieldError('confirmacaoSenha', confirmError);
    showToast(confirmError, "error");
    return;
  }

  try {
    await apiRequest(`/api/usuarios/${state.user.id}/senha`, {
      method: "PUT",
      body: JSON.stringify({
        senhaAtual: data.senhaAtual,
        novaSenha: data.novaSenha,
      }),
    });
    showToast("Senha alterada com sucesso!");
    e.target.reset();
  } catch (err) {
    showToast("Erro ao alterar senha: " + err.message, "error");
  }
}

// exibe form de endereco no perfil
export function showProfileAddressForm(addressId = null) {
  const addr = addressId ? state.addresses.find(a => a.id === addressId) : null;
  const isEdit = !!addr;

  const container = document.getElementById("profile-address-form-container");
  container.innerHTML = `
    <div class="admin-form">
      <h3>${isEdit ? 'Editar' : 'Novo'} Endereco</h3>
      <form onsubmit="handleProfileAddress(event, ${isEdit ? addr.id : 'null'})">
        <div class="form-row">
          <div class="form-group">
            <label>Finalidade do Endereco *</label>
            <select name="tipoEndereco" required>
              <option value="">Selecione</option>
              ${[{v:'ENTREGA',l:'Entrega'},{v:'COBRANCA',l:'Cobranca'},{v:'AMBOS',l:'Entrega e Cobranca'}].map(t =>
                `<option value="${t.v}" ${isEdit && addr.tipoEndereco === t.v ? 'selected' : ''}>${t.l}</option>`
              ).join('')}
            </select>
          </div>
          <div class="form-group">
            <label>Nome do Endereco *</label>
            <input type="text" name="nomeEndereco" placeholder="Ex: Casa, Trabalho" value="${isEdit ? addr.nomeEndereco || '' : ''}" required />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Tipo de Residencia *</label>
            <select name="tipoResidencia" required>
              <option value="">Selecione</option>
              ${['Casa', 'Apartamento', 'Condominio', 'Comercial', 'Outro'].map(t =>
                `<option value="${t}" ${isEdit && addr.tipoResidencia === t ? 'selected' : ''}>${t}</option>`
              ).join('')}
            </select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Tipo de Logradouro *</label>
            <select name="tipoLogradouro" required>
              <option value="">Selecione</option>
              ${['Rua', 'Avenida', 'Travessa', 'Alameda', 'Praca', 'Rodovia', 'Estrada', 'Viela'].map(t =>
                `<option value="${t}" ${isEdit && addr.tipoLogradouro === t ? 'selected' : ''}>${t}</option>`
              ).join('')}
            </select>
          </div>
          <div class="form-group">
            <label>Logradouro *</label>
            <input type="text" name="logradouro" placeholder="Ex: das Flores" value="${isEdit ? addr.logradouro || '' : ''}" required />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Numero *</label>
            <input type="text" name="numero" value="${isEdit ? addr.numero || '' : ''}" required />
          </div>
          <div class="form-group">
            <label>Bairro *</label>
            <input type="text" name="bairro" value="${isEdit ? addr.bairro || '' : ''}" required />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>CEP *</label>
            <input type="text" name="cep" required maxlength="9"
              placeholder="00000-000"
              value="${isEdit ? addr.cep || '' : ''}"
              oninput="this.value=this.value.replace(/[^\\d]/g,'').replace(/(\\d{5})(\\d)/,'$1-$2')" />
          </div>
          <div class="form-group">
            <label>Cidade *</label>
            <input type="text" name="cidade" value="${isEdit ? addr.cidade || '' : ''}" required />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Estado *</label>
            <select name="estado" required>
              <option value="">Selecione</option>
              ${['AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG','PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO'].map(uf =>
                `<option value="${uf}" ${isEdit && addr.estado === uf ? 'selected' : ''}>${uf}</option>`
              ).join('')}
            </select>
          </div>
          <div class="form-group">
            <label>Pais *</label>
            <select name="pais" required>
              <option value="">Selecione</option>
              ${['Brasil', 'Portugal', 'Estados Unidos', 'Argentina', 'Outro'].map(p =>
                `<option value="${p}" ${isEdit && addr.pais === p ? 'selected' : ''}>${p}</option>`
              ).join('')}
            </select>
          </div>
        </div>
        <div class="form-group">
          <label>Observacoes</label>
          <input type="text" name="observacoes" placeholder="Complemento, referencia..." value="${isEdit ? addr.observacoes || '' : ''}" />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">${isEdit ? 'Atualizar' : 'Salvar'} Endereco</button>
          <button type="button" class="btn btn-outline" onclick="hideProfileAddressForm()">Cancelar</button>
        </div>
      </form>
    </div>
  `;
}

export function hideProfileAddressForm() {
  document.getElementById("profile-address-form-container").innerHTML = "";
}

// handler para criar/atualizar endereco no perfil
export async function handleProfileAddress(e, addressId) {
  e.preventDefault();
  const formData = new FormData(e.target);
  const data = Object.fromEntries(formData);

  try {
    if (addressId) {
      await apiRequest(`/api/enderecos/${addressId}`, {
        method: "PUT",
        body: JSON.stringify(data),
      });
      showToast("Endereco atualizado com sucesso!");
    } else {
      await apiRequest("/api/enderecos", {
        method: "POST",
        body: JSON.stringify(data),
      });
      showToast("Endereco salvo com sucesso!");
    }
    await loadAddresses();
    hideProfileAddressForm();
    document.getElementById("profile-content").innerHTML = ProfileEnderecosTab();
  } catch (err) {
    showToast("Erro ao salvar endereco: " + err.message, "error");
  }
}

// editar endereco no perfil
export function editProfileAddress(id) {
  showProfileAddressForm(id);
}

// excluir endereco no perfil
export async function deleteProfileAddress(id) {
  if (!confirm("Tem certeza que deseja excluir este endereco?")) return;
  try {
    await apiRequest(`/api/enderecos/${id}`, { method: "DELETE" });
    showToast("Endereco excluido com sucesso!");
    await loadAddresses();
    document.getElementById("profile-content").innerHTML = ProfileEnderecosTab();
  } catch (err) {
    showToast("Erro ao excluir endereco: " + err.message, "error");
  }
}

// exibe form de cartao no perfil
export function showProfileCardForm() {
  const container = document.getElementById("profile-card-form-container");
  container.innerHTML = `
    <div class="admin-form">
      <h3>Novo Cartao</h3>
      <form onsubmit="handleProfileCard(event)">
        <div class="form-row">
          <div class="form-group">
            <label>Numero do Cartao *</label>
            <input type="text" name="numero" required maxlength="19"
              placeholder="0000 0000 0000 0000"
              oninput="this.value=this.value.replace(/[^\\d\\s-]/g,'')" />
          </div>
          <div class="form-group">
            <label>Nome Impresso *</label>
            <input type="text" name="nomeImpresso" required
              placeholder="Como aparece no cartao"
              oninput="this.value=this.value.replace(/[^a-zA-ZÀ-ÿ\\s]/g,'')" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Bandeira *</label>
            <select name="bandeira" required>
              <option value="">Selecione a bandeira</option>
              <option value="VISA">Visa</option>
              <option value="MASTERCARD">Mastercard</option>
              <option value="ELO">Elo</option>
              <option value="AMEX">American Express</option>
              <option value="HIPERCARD">Hipercard</option>
              <option value="DINERS">Diners Club</option>
            </select>
          </div>
          <div class="form-group">
            <label>Codigo Seguranca *</label>
            <input type="text" name="codigoSeguranca" required maxlength="4"
              placeholder="CVV"
              oninput="this.value=this.value.replace(/[^\\d]/g,'')" />
          </div>
        </div>
        <div class="form-group">
          <label>
            <input type="checkbox" name="preferencial" />
            Cartao Preferencial
          </label>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">Salvar Cartao</button>
          <button type="button" class="btn btn-outline" onclick="hideProfileCardForm()">Cancelar</button>
        </div>
      </form>
    </div>
  `;
}

export function hideProfileCardForm() {
  document.getElementById("profile-card-form-container").innerHTML = "";
}

// handler para criar cartao no perfil
export async function handleProfileCard(e) {
  e.preventDefault();
  const formData = new FormData(e.target);
  const data = Object.fromEntries(formData);

  const payload = {
    numero: data.numero,
    nomeImpresso: data.nomeImpresso,
    bandeira: data.bandeira,
    "codigoSegurança": data.codigoSeguranca,
    preferencial: data.preferencial === "on",
  };

  try {
    await apiRequest("/api/cartoes", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    showToast("Cartao salvo com sucesso!");
    await loadCards();
    hideProfileCardForm();
    document.getElementById("profile-content").innerHTML = ProfileCartoesTab();
  } catch (err) {
    showToast("Erro ao salvar cartao: " + err.message, "error");
  }
}

// exibe form de edicao de cartao no perfil
export function showEditCardForm(id, nomeImpresso, preferencial) {
  const container = document.getElementById("profile-card-form-container");
  container.innerHTML = `
    <div class="admin-form">
      <h3>Editar Cartao</h3>
      <form onsubmit="handleEditCard(event, ${id})">
        <div class="form-row">
          <div class="form-group">
            <label>Nome Impresso *</label>
            <input type="text" name="nomeImpresso" required
              value="${nomeImpresso}"
              placeholder="Como aparece no cartao"
              oninput="this.value=this.value.replace(/[^a-zA-ZÀ-ÿ\\s]/g,'')" />
          </div>
        </div>
        <div class="form-group">
          <label>
            <input type="checkbox" name="preferencial" ${preferencial ? 'checked' : ''} />
            Cartao Preferencial
          </label>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">Salvar Alteracoes</button>
          <button type="button" class="btn btn-outline" onclick="hideProfileCardForm()">Cancelar</button>
        </div>
      </form>
    </div>
  `;
}

// handler para editar cartao no perfil
export async function handleEditCard(e, id) {
  e.preventDefault();
  const formData = new FormData(e.target);
  const data = Object.fromEntries(formData);

  const payload = {
    nomeImpresso: data.nomeImpresso,
    preferencial: data.preferencial === "on",
  };

  try {
    await apiRequest(`/api/cartoes/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
    showToast("Cartao atualizado com sucesso!");
    await loadCards();
    hideProfileCardForm();
    document.getElementById("profile-content").innerHTML = ProfileCartoesTab();
  } catch (err) {
    showToast("Erro ao atualizar cartao: " + err.message, "error");
  }
}

// excluir cartao no perfil
export async function deleteProfileCard(id) {
  if (!confirm("Tem certeza que deseja excluir este cartao?")) return;
  try {
    await apiRequest(`/api/cartoes/${id}`, { method: "DELETE" });
    showToast("Cartao excluido com sucesso!");
    await loadCards();
    document.getElementById("profile-content").innerHTML = ProfileCartoesTab();
  } catch (err) {
    showToast("Erro ao excluir cartao: " + err.message, "error");
  }
}
