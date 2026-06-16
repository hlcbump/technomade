import { state } from '../state.js';
import { apiRequest } from '../api.js';
import { showToast } from '../utils.js';
import { Header } from '../components/header.js';
import { Footer } from '../components/footer.js';
import { getCartTotal, saveCartToStorage } from '../cart.js';
import { loadAddresses, loadCards, calculateShipping } from '../data.js';
import { ensureUserId } from '../auth.js';

// calcula o desconto total dos cupons aplicados
function getTotalCuponsDesconto() {
  return (state.checkoutCupons || []).reduce((sum, c) => sum + (Number(c.valor) || 0), 0);
}

// verifica se ja tem um cupom promocional aplicado
function hasPromocional() {
  return (state.checkoutCupons || []).some(c => c.promocional);
}

// pagina de checkout com selecao de endereco, pagamento combinado e resumo
// RF0033, RF0034, RF0035, RF0036, RF0037, RN0033, RN0034, RN0035, RN0047
export function CheckoutPage() {
  const subtotal = getCartTotal();
  const frete = state.shipping != null ? state.shipping : 0;
  // cupom aplica apenas sobre subtotal, nunca sobre frete
  const cupomDesconto = Math.min(getTotalCuponsDesconto(), subtotal);

  // RN0047 - verificar frete gratis SP
  const selectedAddr = state.addresses.find(a => a.id === Number(state.selectedAddressId));
  const freteGratisSP = selectedAddr && selectedAddr.estado && selectedAddr.estado.toUpperCase() === 'SP';
  const freteEfetivo = freteGratisSP ? 0 : frete;
  const totalFinal = (subtotal - cupomDesconto) + freteEfetivo;

  // total ja pago por cartoes adicionados
  const totalCartoes = (state.checkoutCartoes || []).reduce((sum, cc) => sum + Number(cc.valor), 0);
  const valorRestanteCartao = Math.max(totalFinal - totalCartoes, 0);
  const valorExcedente = Math.max(totalCartoes - totalFinal, 0);

  const cuponsAplicados = state.checkoutCupons || [];
  const cuponsDisponiveis = (state.cupons || []).filter(c => !cuponsAplicados.find(a => a.codigo === c.codigo));

  return `
    ${Header()}
    <main>
      <section class="page-section">
        <div class="container">
          <h1 class="page-title">Finalizar Compra</h1>

          ${
            state.cart.length === 0
              ? `
                <div class="empty-state">
                  <i class="fas fa-shopping-cart"></i>
                  <h3>Seu carrinho esta vazio</h3>
                  <button class="btn btn-primary" onclick="navigate('products')">
                    Ver Produtos
                  </button>
                </div>
              `
              : `
                <div class="checkout-grid">
                  <div class="checkout-steps">
                    <div class="checkout-card">
                      <h3>Endereco de Entrega</h3>
                      ${
                        state.addresses.length > 0
                          ? `
                            <select class="input" onchange="handleAddressChange(this.value)">
                              ${state.addresses
                                .map(
                                  (a) => `
                                    <option value="${a.id}" ${
                                      Number(state.selectedAddressId) === a.id ? "selected" : ""
                                    }>
                                      ${a.nomeEndereco} - ${a.cidade}/${a.estado}
                                    </option>
                                  `
                                )
                                .join("")}
                            </select>
                            ${freteGratisSP ? `<span class="badge-frete-gratis"><i class="fas fa-gift"></i> Frete Gratis para SP!</span>` : ''}
                          `
                          : `<p class="text-muted">Nenhum endereco cadastrado.</p>`
                      }
                      <button class="btn btn-outline btn-sm" onclick="showAddressForm()" style="margin-top: 0.5rem;">
                        Adicionar Endereco
                      </button>
                      <div id="address-form-container"></div>
                    </div>

                    <div class="checkout-card">
                      <h3>Pagamento</h3>

                      <div class="checkout-payment-section">
                        <h4><i class="fas fa-credit-card"></i> Cartoes de Credito (RN0034)</h4>

                        ${(state.checkoutCartoes || []).length > 0 ? `
                          <div style="display: flex; flex-direction: column; gap: 0.4rem; margin-bottom: 0.75rem;">
                            ${state.checkoutCartoes.map((cc, idx) => {
                              const card = state.cards.find(c => c.id === cc.cardId);
                              return card ? `
                                <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.5rem 0.75rem; background: #cce5ff; border-radius: var(--radius-sm); font-size: 0.813rem;">
                                  <span>${card.bandeira} **** ${card.numero.slice(-4)} — R$ ${Number(cc.valor).toFixed(2)}</span>
                                  <button class="btn-icon text-danger" onclick="removeCheckoutCard(${idx})" title="Remover" style="width: 28px; height: 28px;">
                                    <i class="fas fa-times"></i>
                                  </button>
                                </div>
                              ` : '';
                            }).join('')}
                          </div>
                        ` : ''}

                        ${state.cards.length > 0 ? `
                          <div style="display: flex; gap: 0.5rem; align-items: center; flex-wrap: wrap;">
                            <select class="input" id="checkout-card-select" style="flex: 1; min-width: 180px;">
                              ${state.cards
                                .filter(c => !(state.checkoutCartoes || []).find(cc => cc.cardId === c.id))
                                .map(c => `
                                  <option value="${c.id}">
                                    ${c.bandeira} **** ${c.numero.slice(-4)} ${c.preferencial ? "(Preferencial)" : ""}
                                  </option>
                                `).join("")}
                            </select>
                            <input type="number" id="checkout-card-valor" placeholder="Valor R$" step="0.01" min="0.01"
                                   style="width: 120px; padding: 0.5rem 0.75rem; border: 2px solid var(--gray-light); border-radius: var(--radius-sm); font-size: 0.875rem;" />
                            <button class="btn btn-primary btn-sm" onclick="addCheckoutCard()">Adicionar</button>
                          </div>
                          <p class="text-muted" style="font-size: 0.75rem; margin-top: 0.25rem;">
                            Min R$ 10,00 por cartao. Pode usar varios cartoes.
                            ${valorRestanteCartao > 0 ? `Valor restante: <strong>R$ ${valorRestanteCartao.toFixed(2)}</strong>` : valorExcedente > 0.01 ? `<strong style="color: var(--danger);">Valor excede o total em R$ ${valorExcedente.toFixed(2)}! Ajuste os cartoes.</strong>` : '<strong style="color: var(--success);">Valor total coberto!</strong>'}
                          </p>
                        ` : `<p class="text-muted">Nenhum cartao cadastrado.</p>`}
                        <button class="btn btn-outline btn-sm" onclick="showCardForm()" style="margin-top: 0.5rem;">
                          Adicionar Cartao
                        </button>
                        <div id="card-form-container"></div>
                      </div>

                      <div class="divider"></div>

                      <div class="checkout-payment-section">
                        <h4><i class="fas fa-ticket-alt"></i> Cupons de Desconto</h4>

                        ${cuponsAplicados.length > 0 ? `
                          <div style="display: flex; flex-direction: column; gap: 0.4rem; margin-bottom: 0.75rem;">
                            ${cuponsAplicados.map(c => `
                              <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.5rem 0.75rem; background: #d4edda; border-radius: var(--radius-sm); font-size: 0.813rem;">
                                <span>
                                  <i class="fas fa-check" style="color: #155724;"></i>
                                  <strong>${c.codigo}</strong> — R$ ${Number(c.valor).toFixed(2)}
                                  (${c.promocional ? 'Promocional' : 'Troca'})
                                </span>
                                <button class="btn-icon text-danger" onclick="removeCoupon('${c.codigo}')" title="Remover" style="width: 28px; height: 28px;">
                                  <i class="fas fa-times"></i>
                                </button>
                              </div>
                            `).join('')}
                          </div>
                        ` : ''}

                        <div style="display: flex; gap: 0.5rem; align-items: center;">
                          <input type="text" id="coupon-input" placeholder="Codigo do cupom" class="input"
                                 style="flex: 1; padding: 0.5rem 0.75rem; border: 2px solid var(--gray-light); border-radius: var(--radius-sm);" />
                          <button class="btn btn-outline btn-sm" onclick="applyCoupon()">Aplicar</button>
                        </div>
                        <p class="text-muted" style="font-size: 0.75rem; margin-top: 0.25rem;">
                          Cupons de troca (ilimitados) + max 1 cupom promocional (RN0033)
                        </p>

                        ${cuponsDisponiveis.length > 0 ? `
                          <div style="margin-top: 0.75rem;">
                            <p style="font-size: 0.813rem; font-weight: 600; margin-bottom: 0.5rem;">Seus cupons disponiveis:</p>
                            <div style="display: flex; flex-direction: column; gap: 0.4rem;">
                              ${cuponsDisponiveis.map(c => `
                                <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.5rem 0.75rem; background: var(--gray-light); border-radius: var(--radius-sm); font-size: 0.813rem;">
                                  <span><strong>${c.codigo}</strong> — R$ ${Number(c.valor).toFixed(2)} (${c.promocional ? 'Promocional' : 'Troca'})</span>
                                  <button class="btn btn-sm btn-primary" style="padding: 0.2rem 0.6rem; font-size: 0.75rem;" onclick="document.getElementById('coupon-input').value='${c.codigo}'; applyCoupon();">Usar</button>
                                </div>
                              `).join('')}
                            </div>
                          </div>
                        ` : ''}
                      </div>
                    </div>
                  </div>

                  <div class="checkout-summary">
                    <h3>Resumo</h3>
                    <div class="summary-row">
                      <span>Subtotal:</span>
                      <span>R$ ${subtotal.toFixed(2)}</span>
                    </div>
                    <div class="summary-row">
                      <span>Frete:</span>
                      <span>${
                        freteGratisSP
                          ? '<span style="text-decoration: line-through; color: var(--gray);">R$ ' + frete.toFixed(2) + '</span> <strong style="color: var(--success);">Gratis</strong>'
                          : (state.shipping != null ? `R$ ${frete.toFixed(2)}` : "A calcular")
                      }</span>
                    </div>
                    ${cupomDesconto > 0 ? `
                      <div class="summary-row" style="color: var(--success);">
                        <span>Cupons (${cuponsAplicados.length}):</span>
                        <span>- R$ ${cupomDesconto.toFixed(2)}</span>
                      </div>
                    ` : ''}
                    <div class="divider"></div>
                    <div class="summary-row summary-total">
                      <span>Total:</span>
                      <span>R$ ${totalFinal.toFixed(2)}</span>
                    </div>

                    ${(state.checkoutCartoes || []).length > 0 ? `
                      <div class="divider"></div>
                      <p style="font-size: 0.75rem; font-weight: 600; margin-bottom: 0.25rem;">Pagamento com cartoes:</p>
                      ${state.checkoutCartoes.map(cc => {
                        const card = state.cards.find(c => c.id === cc.cardId);
                        return card ? `
                          <div class="summary-row" style="font-size: 0.813rem;">
                            <span>${card.bandeira} **** ${card.numero.slice(-4)}</span>
                            <span>R$ ${Number(cc.valor).toFixed(2)}</span>
                          </div>
                        ` : '';
                      }).join('')}
                      ${valorRestanteCartao > 0 ? `
                        <div class="summary-row" style="font-size: 0.813rem; color: var(--danger);">
                          <span>Falta:</span>
                          <span>R$ ${valorRestanteCartao.toFixed(2)}</span>
                        </div>
                      ` : ''}
                    ` : ''}

                    ${totalFinal > 0 && totalFinal < 10 && cuponsAplicados.length === 0 && (state.checkoutCartoes || []).length === 0 ? `
                      <p style="color: var(--danger); font-size: 0.813rem; margin-top: 0.5rem;">
                        <i class="fas fa-exclamation-triangle"></i> Valor minimo de R$ 10,00 por cartao (RN0034)
                      </p>
                    ` : ''}

                    <button class="btn btn-primary btn-block" onclick="finalizarCompra()" style="margin-top: 1rem;">
                      Confirmar Compra
                    </button>
                    <button class="btn btn-outline btn-block" onclick="navigate('cart')">
                      Voltar ao Carrinho
                    </button>
                  </div>
                </div>
              `
          }
        </div>
      </section>
    </main>
    ${Footer()}
  `;
}

// atualiza o endereco selecionado e recalcula o frete
export async function handleAddressChange(value) {
  const id = Number(value);
  state.selectedAddressId = id;
  await calculateShipping(id);
  window.render();
}

// atualiza o cartao selecionado no checkout
export function handleCardChange(value) {
  state.selectedCardId = Number(value);
}

// aplica cupom de desconto - RF0036, RN0033, RN0035
export async function applyCoupon() {
  const input = document.getElementById('coupon-input');
  const codigo = input ? input.value.trim() : '';
  if (!codigo) {
    showToast("Digite o codigo do cupom.", "error");
    return;
  }

  // verificar se ja foi aplicado
  if ((state.checkoutCupons || []).find(c => c.codigo.toUpperCase() === codigo.toUpperCase())) {
    showToast("Este cupom ja foi aplicado.", "error");
    return;
  }

  try {
    const cupom = await apiRequest(`/api/cupons/validar/${codigo}`);
    if (!cupom) {
      showToast("Cupom invalido.", "error");
      return;
    }

    // RN0033 - max 1 cupom promocional
    if (cupom.promocional && hasPromocional()) {
      showToast("Ja existe um cupom promocional aplicado. Maximo 1 por compra (RN0033).", "error");
      return;
    }

    // RN0036 - nao permitir uso desnecessario de cupons
    const subtotal = getCartTotal();
    const frete = state.shipping != null ? state.shipping : 0;
    const selectedAddr = state.addresses.find(a => a.id === Number(state.selectedAddressId));
    const freteGratisSP = selectedAddr && selectedAddr.estado && selectedAddr.estado.toUpperCase() === 'SP';
    const freteEfetivo = freteGratisSP ? 0 : frete;
    // cupom aplica apenas sobre subtotal, nao sobre frete
    const descontoAtual = getTotalCuponsDesconto();

    if (descontoAtual >= subtotal) {
      showToast("Os cupons aplicados ja cobrem o valor total da compra.", "error");
      return;
    }

    if (!state.checkoutCupons) state.checkoutCupons = [];
    state.checkoutCupons.push(cupom);
    ajustarCartoesAposCupom();
    showToast("Cupom aplicado com sucesso!");
    window.render();
  } catch (err) {
    showToast("Cupom invalido: " + err.message, "error");
  }
}

// adiciona um cartao ao pagamento do checkout - RN0034
export function addCheckoutCard() {
  const select = document.getElementById('checkout-card-select');
  const valorInput = document.getElementById('checkout-card-valor');
  if (!select || !valorInput) return;

  const cardId = Number(select.value);
  const valor = parseFloat(valorInput.value);

  if (!cardId) {
    showToast("Selecione um cartao.", "error");
    return;
  }
  if (!valor || valor <= 0) {
    showToast("Digite um valor valido.", "error");
    return;
  }

  // RN0034 - min R$10 por cartao, exceto quando combinado com cupons (RN0035)
  const temCupom = (state.checkoutCupons || []).length > 0;
  if (valor < 10.0 && !temCupom) {
    showToast("O valor minimo por cartao e R$ 10,00 (RN0034).", "error");
    return;
  }

  // verificar se cartao ja foi adicionado
  if ((state.checkoutCartoes || []).find(cc => cc.cardId === cardId)) {
    showToast("Este cartao ja foi adicionado.", "error");
    return;
  }

  if (!state.checkoutCartoes) state.checkoutCartoes = [];
  state.checkoutCartoes.push({ cardId, valor });
  window.render();
}

// remove um cartao do checkout pelo indice
export function removeCheckoutCard(idx) {
  if (!state.checkoutCartoes) return;
  state.checkoutCartoes.splice(idx, 1);
  window.render();
}

// remove cupom aplicado pelo codigo
export function removeCoupon(codigo) {
  state.checkoutCupons = (state.checkoutCupons || []).filter(c => c.codigo !== codigo);
  ajustarCartoesAposCupom();
  window.render();
}

// ajusta o valor do ultimo cartao para que a soma dos cartoes coincida com o total apos cupom
function ajustarCartoesAposCupom() {
  const cartoes = state.checkoutCartoes || [];
  if (cartoes.length === 0) return;

  const subtotal = getCartTotal();
  const frete = state.shipping != null ? state.shipping : 0;
  const selectedAddr = state.addresses.find(a => a.id === Number(state.selectedAddressId));
  const freteGratisSP = selectedAddr && selectedAddr.estado && selectedAddr.estado.toUpperCase() === 'SP';
  const freteEfetivo = freteGratisSP ? 0 : frete;
  const cupomDesconto = Math.min(getTotalCuponsDesconto(), subtotal);
  const totalFinal = Math.max((subtotal - cupomDesconto) + freteEfetivo, 0);

  const totalCartoes = cartoes.reduce((sum, cc) => sum + Number(cc.valor), 0);
  if (Math.abs(totalCartoes - totalFinal) < 0.01) return; // ja esta correto

  // ajusta o ultimo cartao para compensar a diferenca
  const ultimo = cartoes[cartoes.length - 1];
  const diff = totalCartoes - totalFinal;
  const novoValor = Number(ultimo.valor) - diff;

  if (novoValor >= 0.01) {
    ultimo.valor = Math.round(novoValor * 100) / 100;
  } else {
    // se o ultimo cartao ficaria zerado/negativo, remove-o
    cartoes.pop();
    // tenta ajustar recursivamente se ainda ha cartoes
    if (cartoes.length > 0) ajustarCartoesAposCupom();
  }
}

// finaliza a compra enviando o pedido para a API - RF0037, RN0034, RN0035
export async function finalizarCompra() {
  if (!state.isAuthenticated) {
    showToast("Voce precisa estar logado para finalizar a compra.", "error");
    return;
  }

  if (!state.selectedAddressId) {
    showToast("Selecione um endereco de entrega.", "error");
    return;
  }

  const cuponsAplicados = state.checkoutCupons || [];
  const cartoesAdicionados = state.checkoutCartoes || [];

  if (cartoesAdicionados.length === 0 && cuponsAplicados.length === 0) {
    showToast("Adicione pelo menos uma forma de pagamento.", "error");
    return;
  }

  const userId = await ensureUserId();
  if (!userId) {
    showToast("Nao foi possivel identificar o usuario.", "error");
    return;
  }

  if (state.shipping == null) {
    await calculateShipping(state.selectedAddressId);
  }

  const subtotal = getCartTotal();
  const frete = state.shipping != null ? state.shipping : 0;

  // RN0047 - frete gratis para SP
  const selectedAddr = state.addresses.find(a => a.id === Number(state.selectedAddressId));
  const freteGratisSP = selectedAddr && selectedAddr.estado && selectedAddr.estado.toUpperCase() === 'SP';
  const freteEfetivo = freteGratisSP ? 0 : frete;

  // cupom aplica apenas sobre subtotal, frete sempre cobrado
  const cupomDesconto = Math.min(getTotalCuponsDesconto(), subtotal);
  const totalFinal = Math.max((subtotal - cupomDesconto) + freteEfetivo, 0);

  // monta pagamentos combinados - RN0035
  const pagamentos = [];

  // adiciona cada cupom como pagamento separado
  let descontoRestante = subtotal;
  for (const cupom of cuponsAplicados) {
    if (descontoRestante <= 0) break;
    const valorCupom = Math.min(Number(cupom.valor), descontoRestante);
    pagamentos.push({
      formaPagamento: cupom.promocional ? 'CUPOM_PROMOCIONAL' : 'CUPOM_TROCA',
      cupomCodigo: cupom.codigo,
      valor: valorCupom,
    });
    descontoRestante -= valorCupom;
  }

  // adiciona cada cartao como pagamento separado - RN0034
  const totalCartoes = cartoesAdicionados.reduce((sum, cc) => sum + Number(cc.valor), 0);

  if (totalFinal > 0 && cartoesAdicionados.length === 0) {
    showToast("Adicione pelo menos um cartao para pagar o valor restante.", "error");
    return;
  }

  // verificar se a soma dos cartoes cobre o total
  const tolerancia = 0.01;
  if (totalFinal > 0 && Math.abs(totalCartoes - totalFinal) > tolerancia) {
    showToast(`A soma dos cartoes (R$ ${totalCartoes.toFixed(2)}) deve ser igual ao total (R$ ${totalFinal.toFixed(2)}).`, "error");
    return;
  }

  for (const cc of cartoesAdicionados) {
    pagamentos.push({
      formaPagamento: "CARTAO_CREDITO",
      cartaoCreditoId: cc.cardId,
      valor: Number(cc.valor),
    });
  }

  const payload = {
    enderecoEntregaId: state.selectedAddressId,
    pagamentos,
  };

  try {
    await apiRequest("/api/compras", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    showToast("Compra realizada com sucesso!");
    state.cart = [];
    state.checkoutCupons = [];
    state.checkoutCartoes = [];
    saveCartToStorage();
    window.navigate("orders");
  } catch (err) {
    showToast("Erro ao finalizar compra: " + err.message, "error");
  }
}

// exibe o formulario para cadastrar um novo endereco no checkout
export function showAddressForm() {
  const container = document.getElementById("address-form-container");
  container.innerHTML = `
    <div class="admin-form">
      <h4>Novo Endereco</h4>
      <form onsubmit="handleCreateAddress(event)">
        <div class="form-row">
          <div class="form-group">
            <label>Finalidade *</label>
            <select name="tipoEndereco" required>
              <option value="">Selecione</option>
              <option value="ENTREGA" selected>Entrega</option>
              <option value="COBRANCA">Cobranca</option>
              <option value="AMBOS">Entrega e Cobranca</option>
            </select>
          </div>
          <div class="form-group">
            <label>Nome do Endereco *</label>
            <input type="text" name="nomeEndereco" placeholder="Ex: Casa, Trabalho" required />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Tipo de Residencia *</label>
            <select name="tipoResidencia" required>
              <option value="">Selecione</option>
              <option value="Casa">Casa</option>
              <option value="Apartamento">Apartamento</option>
              <option value="Condominio">Condominio</option>
              <option value="Comercial">Comercial</option>
              <option value="Outro">Outro</option>
            </select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Tipo de Logradouro *</label>
            <select name="tipoLogradouro" required>
              <option value="">Selecione</option>
              <option value="Rua">Rua</option>
              <option value="Avenida">Avenida</option>
              <option value="Travessa">Travessa</option>
              <option value="Alameda">Alameda</option>
              <option value="Praca">Praca</option>
              <option value="Rodovia">Rodovia</option>
              <option value="Estrada">Estrada</option>
              <option value="Viela">Viela</option>
            </select>
          </div>
          <div class="form-group">
            <label>Logradouro *</label>
            <input type="text" name="logradouro" placeholder="Ex: das Flores" required />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Numero *</label>
            <input type="text" name="numero" required />
          </div>
          <div class="form-group">
            <label>Bairro *</label>
            <input type="text" name="bairro" required />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>CEP *</label>
            <input type="text" name="cep" required maxlength="9"
              placeholder="00000-000"
              oninput="this.value=this.value.replace(/[^\\d]/g,'').replace(/(\\d{5})(\\d)/,'$1-$2')" />
          </div>
          <div class="form-group">
            <label>Cidade *</label>
            <input type="text" name="cidade" required />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Estado *</label>
            <select name="estado" required>
              <option value="">Selecione</option>
              <option value="AC">AC</option><option value="AL">AL</option><option value="AP">AP</option>
              <option value="AM">AM</option><option value="BA">BA</option><option value="CE">CE</option>
              <option value="DF">DF</option><option value="ES">ES</option><option value="GO">GO</option>
              <option value="MA">MA</option><option value="MT">MT</option><option value="MS">MS</option>
              <option value="MG">MG</option><option value="PA">PA</option><option value="PB">PB</option>
              <option value="PR">PR</option><option value="PE">PE</option><option value="PI">PI</option>
              <option value="RJ">RJ</option><option value="RN">RN</option><option value="RS">RS</option>
              <option value="RO">RO</option><option value="RR">RR</option><option value="SC">SC</option>
              <option value="SP">SP</option><option value="SE">SE</option><option value="TO">TO</option>
            </select>
          </div>
          <div class="form-group">
            <label>Pais *</label>
            <select name="pais" required>
              <option value="">Selecione</option>
              <option value="Brasil" selected>Brasil</option>
              <option value="Portugal">Portugal</option>
              <option value="Estados Unidos">Estados Unidos</option>
              <option value="Argentina">Argentina</option>
              <option value="Outro">Outro</option>
            </select>
          </div>
        </div>
        <div class="form-group">
          <label>Observacoes</label>
          <input type="text" name="observacoes" placeholder="Complemento, referencia..." />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">Salvar Endereco</button>
          <button type="button" class="btn btn-outline" onclick="hideAddressForm()">Cancelar</button>
        </div>
      </form>
    </div>
  `;
}

// esconde o formulario de endereco
export function hideAddressForm() {
  document.getElementById("address-form-container").innerHTML = "";
}

// handler para criar endereco - envia para API e recalcula frete
export async function handleCreateAddress(e) {
  e.preventDefault();
  const form = e.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData);

  try {
    await apiRequest("/api/enderecos", {
      method: "POST",
      body: JSON.stringify(data),
    });
    showToast("Endereco salvo com sucesso!");
    await loadAddresses();
    if (state.addresses.length > 0) {
      state.selectedAddressId = state.addresses[state.addresses.length - 1].id;
      await calculateShipping(state.selectedAddressId);
    }
    hideAddressForm();
    window.render();
  } catch (err) {
    showToast("Erro ao salvar endereco: " + err.message, "error");
  }
}

// exibe o formulario para cadastrar um novo cartao no checkout
export function showCardForm() {
  const container = document.getElementById("card-form-container");
  container.innerHTML = `
    <div class="admin-form">
      <h4>Novo Cartao</h4>
      <form onsubmit="handleCreateCard(event)">
        <div class="form-row">
          <div class="form-group">
            <label>Numero do Cartao</label>
            <input type="text" name="numero" required maxlength="19"
              placeholder="0000 0000 0000 0000"
              oninput="this.value=this.value.replace(/[^\\d\\s-]/g,'')" />
          </div>
          <div class="form-group">
            <label>Nome Impresso</label>
            <input type="text" name="nomeImpresso" required
              placeholder="Como aparece no cartao"
              oninput="this.value=this.value.replace(/[^a-zA-ZÀ-ÿ\\s]/g,'')" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Bandeira</label>
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
            <label>Codigo Seguranca</label>
            <input type="text" name="codigoSeguranca" required maxlength="4"
              placeholder="CVV"
              oninput="this.value=this.value.replace(/[^\\d]/g,'')" />
          </div>
        </div>
        <div class="form-group">
          <label>
            <input type="checkbox" name="preferencial" />
            Preferencial
          </label>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">Salvar Cartao</button>
          <button type="button" class="btn btn-outline" onclick="hideCardForm()">Cancelar</button>
        </div>
      </form>
    </div>
  `;
}

// esconde o formulario de cartao
export function hideCardForm() {
  document.getElementById("card-form-container").innerHTML = "";
}

// handler para criar cartao - envia para API e seleciona automaticamente
export async function handleCreateCard(e) {
  e.preventDefault();
  const form = e.target;
  const formData = new FormData(form);
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
    if (state.cards.length > 0) {
      const preferencial = state.cards.find((c) => c.preferencial);
      state.selectedCardId = preferencial ? preferencial.id : state.cards[0].id;
    }
    hideCardForm();
    window.render();
  } catch (err) {
    showToast("Erro ao salvar cartao: " + err.message, "error");
  }
}
