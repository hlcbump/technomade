import { apiRequest } from '../api.js';

let conversationHistory = [];
let chatbotInitialized = false;
const MAX_HISTORICO = 20;

export function ChatbotWidget() {
  return `
    <div id="chatbot-container">
      <button id="chatbot-toggle" class="chatbot-bubble" title="Assistente Virtual">
        <i class="fas fa-robot"></i>
      </button>
      <div id="chatbot-window" class="chatbot-window chatbot-hidden">
        <div class="chatbot-header">
          <div class="chatbot-header-info">
            <i class="fas fa-robot"></i>
            <span>Assistente Technomade</span>
          </div>
          <button class="chatbot-close" id="chatbot-close-btn">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div id="chatbot-messages" class="chatbot-messages">
          <div class="chatbot-msg chatbot-msg-bot">
            Ola! Sou o assistente da Technomade. Posso te ajudar a encontrar o gadget ideal para seu estilo de vida nomade. O que voce procura?
          </div>
        </div>
        <div class="chatbot-input-area">
          <input type="text" id="chatbot-input" placeholder="Digite sua pergunta..." />
          <button id="chatbot-send">
            <i class="fas fa-paper-plane"></i>
          </button>
        </div>
      </div>
    </div>
  `;
}

export function initChatbot() {
  if (chatbotInitialized) return;

  const existing = document.getElementById('chatbot-container');
  if (!existing) return;

  const toggle = document.getElementById('chatbot-toggle');
  const closeBtn = document.getElementById('chatbot-close-btn');
  const sendBtn = document.getElementById('chatbot-send');
  const input = document.getElementById('chatbot-input');

  if (toggle) {
    toggle.addEventListener('click', () => {
      const win = document.getElementById('chatbot-window');
      if (win) {
        win.classList.toggle('chatbot-hidden');
        if (!win.classList.contains('chatbot-hidden')) {
          const inp = document.getElementById('chatbot-input');
          if (inp) inp.focus();
        }
      }
    });
  }

  if (closeBtn) {
    closeBtn.addEventListener('click', () => {
      const win = document.getElementById('chatbot-window');
      if (win) win.classList.add('chatbot-hidden');
    });
  }

  if (sendBtn) {
    sendBtn.addEventListener('click', sendMessage);
  }

  if (input) {
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    });
  }

  chatbotInitialized = true;
}

async function sendMessage() {
  const input = document.getElementById('chatbot-input');
  const messages = document.getElementById('chatbot-messages');
  if (!input || !messages) return;

  const texto = input.value.trim();
  if (!texto) return;

  input.value = '';

  appendMessage(messages, texto, 'user');

  const typing = showTypingIndicator(messages);

  try {
    const historicoRecente = conversationHistory.slice(-MAX_HISTORICO);

    const resposta = await apiRequest('/api/chatbot/mensagem', {
      method: 'POST',
      body: JSON.stringify({
        mensagem: texto,
        historico: historicoRecente
      })
    });

    removeTypingIndicator(typing);

    appendMessage(messages, resposta.resposta, 'bot');

    if (resposta.produtosReferenciados && resposta.produtosReferenciados.length > 0) {
      appendProductCards(messages, resposta.produtosReferenciados);
    }

    conversationHistory.push({ papel: 'usuario', conteudo: texto });
    conversationHistory.push({ papel: 'assistente', conteudo: resposta.resposta });

  } catch (error) {
    removeTypingIndicator(typing);
    const errorDiv = document.createElement('div');
    errorDiv.className = 'chatbot-error';
    errorDiv.textContent = 'Desculpe, ocorreu um erro ao processar sua mensagem. Tente novamente.';
    messages.appendChild(errorDiv);
  }

  messages.scrollTop = messages.scrollHeight;
}

function appendMessage(container, text, type) {
  const div = document.createElement('div');
  div.className = `chatbot-msg chatbot-msg-${type}`;
  if (type === 'bot') {
    div.innerHTML = formatBotMessage(text);
  } else {
    div.textContent = text;
  }
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
}

function formatBotMessage(text) {
  let safe = escapeHtml(text);
  safe = safe.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  safe = safe.replace(/\n/g, '<br>');
  return safe;
}

function appendProductCards(container, produtos) {
  const cardsDiv = document.createElement('div');
  cardsDiv.className = 'chatbot-product-cards';

  produtos.forEach(produto => {
    const card = document.createElement('div');
    card.className = 'chatbot-product-card';
    card.innerHTML = `
      <img src="${escapeHtml(produto.imagemUrl || '')}" alt="${escapeHtml(produto.nome)}"
           onerror="this.src='https://via.placeholder.com/48?text=Produto'" />
      <div class="chatbot-product-info">
        <span class="chatbot-product-name">${escapeHtml(produto.nome)}</span>
        <span class="chatbot-product-price">R$ ${produto.valorVenda.toFixed(2)}</span>
      </div>
      <button class="btn btn-primary btn-sm" title="Adicionar ao carrinho">
        <i class="fas fa-cart-plus"></i>
      </button>
    `;

    card.querySelector('.chatbot-product-info').addEventListener('click', () => {
      if (window.navigate) window.navigate('products');
    });

    card.querySelector('.btn').addEventListener('click', (e) => {
      e.stopPropagation();
      if (window.addToCart) {
        window.addToCart({
          id: produto.id,
          nome: produto.nome,
          descricao: produto.descricao,
          marca: produto.marca,
          imagemUrl: produto.imagemUrl,
          valorVenda: produto.valorVenda
        });
      }
    });

    cardsDiv.appendChild(card);
  });

  container.appendChild(cardsDiv);
  container.scrollTop = container.scrollHeight;
}

function showTypingIndicator(container) {
  const typing = document.createElement('div');
  typing.className = 'chatbot-typing';
  typing.innerHTML = '<span></span><span></span><span></span>';
  container.appendChild(typing);
  container.scrollTop = container.scrollHeight;
  return typing;
}

function removeTypingIndicator(element) {
  if (element && element.parentNode) {
    element.parentNode.removeChild(element);
  }
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}
