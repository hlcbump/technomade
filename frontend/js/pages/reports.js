import { state } from '../state.js';
import { apiRequest } from '../api.js';
import { showToast } from '../utils.js';
import { Header } from '../components/header.js';
import { Footer } from '../components/footer.js';

let lineChartInstance = null;

const CORES_CATEGORIAS = [
  '#2563eb', '#dc2626', '#16a34a', '#f59e0b', '#8b5cf6',
  '#ec4899', '#06b6d4', '#f97316', '#6366f1', '#14b8a6',
  '#e11d48', '#84cc16', '#0ea5e9', '#a855f7', '#ef4444',
];

export function ReportsPage() {
  if (!state.isAuthenticated || state.user.role !== "ADMIN") {
    return `
      ${Header()}
      <main>
        <section class="page-section">
          <div class="container">
            <div class="empty-state">
              <i class="fas fa-lock"></i>
              <h3>Acesso Negado</h3>
              <p>Voce precisa ser administrador para acessar os relatorios</p>
              <button class="btn btn-primary" onclick="navigate('home')">Voltar ao Inicio</button>
            </div>
          </div>
        </section>
      </main>
      ${Footer()}
    `;
  }

  const reportData = state.reportData || {};
  const categoriaData = state.reportCategorias || [];

  return `
    ${Header()}
    <main>
      <section class="page-section">
        <div class="container">
          <h1 class="page-title">Relatorios de Vendas</h1>

          <div class="admin-section" style="margin-bottom: 2rem;">
            <h3>Filtrar por Periodo</h3>
            <form onsubmit="handleLoadReport(event)" style="display: flex; gap: 1rem; align-items: flex-end; flex-wrap: wrap;">
              <div class="form-group" style="margin-bottom: 0;">
                <label>Data Inicio</label>
                <input type="text" name="dataInicio" id="report-data-inicio" placeholder="dd/mm/aaaa" maxlength="10" required />
              </div>
              <div class="form-group" style="margin-bottom: 0;">
                <label>Data Fim</label>
                <input type="text" name="dataFim" id="report-data-fim" placeholder="dd/mm/aaaa" maxlength="10" required />
              </div>
              <button type="submit" class="btn btn-primary">
                <i class="fas fa-search"></i> Gerar Relatorio
              </button>
            </form>
          </div>

          <div id="report-content">
            ${reportData.vendas ? renderReportContent(reportData, categoriaData, state.reportSerieTemporal) : `
              <div class="empty-state">
                <i class="fas fa-chart-line"></i>
                <h3>Selecione um periodo</h3>
                <p>Defina as datas de inicio e fim para gerar o relatorio</p>
              </div>
            `}
          </div>
        </div>
      </section>
    </main>
    ${Footer()}
  `;
}

function renderReportContent(reportData, categoriaData, serieTemporal) {
  const vendas = reportData.vendas || [];
  const totalCompras = serieTemporal?.totalCompras || 0;

  return `
    <div class="report-grid">
      <div class="report-stats">
        <div class="report-stat-card">
          <div class="report-stat-icon"><i class="fas fa-shopping-cart"></i></div>
          <div class="report-stat-value">${totalCompras}</div>
          <div class="report-stat-label">Total de Compras</div>
        </div>
        <div class="report-stat-card">
          <div class="report-stat-icon"><i class="fas fa-box"></i></div>
          <div class="report-stat-value">${vendas.reduce((s, v) => s + (v.quantidadeVendida || 0), 0)}</div>
          <div class="report-stat-label">Unidades Vendidas</div>
        </div>
        <div class="report-stat-card">
          <div class="report-stat-icon"><i class="fas fa-dollar-sign"></i></div>
          <div class="report-stat-value">R$ ${calcularTotal(vendas)}</div>
          <div class="report-stat-label">Faturamento</div>
        </div>
      </div>

      <div class="admin-section" style="margin-top: 2rem;">
        <h3><i class="fas fa-chart-line"></i> Volume de Vendas por Categoria</h3>
        <div id="line-chart-categorias-filter" class="line-chart-filter"></div>
        <div class="line-chart-container">
          <canvas id="line-chart-categorias"></canvas>
        </div>
      </div>

      ${categoriaData.length > 0 ? `
        <div class="admin-section" style="margin-top: 2rem;">
          <h3>Resumo por Categoria</h3>
          <div class="report-chart" id="report-chart">
            ${renderBarChart(categoriaData)}
          </div>
          <div class="admin-table" style="margin-top: 1rem;">
            <table>
              <thead>
                <tr>
                  <th>Categoria</th>
                  <th>Quantidade</th>
                  <th>Valor Total</th>
                </tr>
              </thead>
              <tbody>
                ${categoriaData.map(c => `
                  <tr>
                    <td>${c.categoria || c.nome || c.nomeCategoria || '-'}</td>
                    <td>${c.quantidade || c.totalQuantidade || 0}</td>
                    <td>R$ ${formatarBRL(c.valorTotal || c.totalValor || 0)}</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          </div>
        </div>
      ` : ''}

      ${vendas.length > 0 ? `
        <div class="admin-section" style="margin-top: 2rem;">
          <h3>Vendas por Produto</h3>
          <div class="admin-table">
            <table>
              <thead>
                <tr>
                  <th>Produto</th>
                  <th>Quantidade Vendida</th>
                  <th>Valor Total</th>
                </tr>
              </thead>
              <tbody>
                ${vendas.map(v => `
                  <tr>
                    <td>${v.nomeProduto || '-'}</td>
                    <td>${v.quantidadeVendida || 0}</td>
                    <td>R$ ${formatarBRL(v.valorTotal || 0)}</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          </div>
        </div>
      ` : ''}
    </div>
  `;
}

function renderBarChart(categoriaData) {
  if (categoriaData.length === 0) return '';
  const maxVal = Math.max(...categoriaData.map(c => Number(c.valorTotal || c.totalValor) || 0), 1);

  return `
    <div class="bar-chart">
      ${categoriaData.map(c => {
        const val = Number(c.valorTotal || c.totalValor) || 0;
        const pct = (val / maxVal) * 100;
        return `
          <div class="bar-chart-item">
            <div class="bar-chart-label">${c.categoria || c.nome || c.nomeCategoria || '-'}</div>
            <div class="bar-chart-bar-container">
              <div class="bar-chart-bar" style="width: ${pct}%"></div>
              <span class="bar-chart-value">R$ ${formatarBRL(val)}</span>
            </div>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

function formatarBRL(valor) {
  return Number(valor).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function calcularTotal(vendas) {
  const total = vendas.reduce((sum, v) => sum + (Number(v.valorTotal) || 0), 0);
  return formatarBRL(total);
}

function calcularTicketMedio(vendas) {
  if (vendas.length === 0) return '0,00';
  const total = vendas.reduce((sum, v) => sum + (Number(v.valorTotal) || 0), 0);
  return formatarBRL(total / vendas.length);
}

function renderLineChart(serieData) {
  const canvas = document.getElementById('line-chart-categorias');
  const filterContainer = document.getElementById('line-chart-categorias-filter');
  if (!canvas || !filterContainer || !serieData || !serieData.series) return;

  const categoriasAtivas = new Set(serieData.series.map(s => s.categoria));

  filterContainer.innerHTML = `
    <div class="line-chart-filter-chips">
      ${serieData.series.map((s, i) => {
        const cor = CORES_CATEGORIAS[i % CORES_CATEGORIAS.length];
        return `
          <label class="chip-categoria" style="--chip-color: ${cor};">
            <input type="checkbox" value="${s.categoria}" checked />
            <span class="chip-dot" style="background: ${cor};"></span>
            ${s.categoria}
          </label>
        `;
      }).join('')}
    </div>
  `;

  filterContainer.querySelectorAll('input[type="checkbox"]').forEach(cb => {
    cb.addEventListener('change', () => {
      const checked = filterContainer.querySelectorAll('input[type="checkbox"]:checked');
      const ativas = new Set(Array.from(checked).map(c => c.value));
      updateChartVisibility(serieData, ativas);
    });
  });

  buildChart(canvas, serieData, categoriasAtivas);
}

function buildChart(canvas, serieData, categoriasAtivas) {
  if (lineChartInstance) {
    lineChartInstance.destroy();
    lineChartInstance = null;
  }

  const datasets = serieData.series.map((s, i) => ({
    label: s.categoria,
    data: s.quantidades,
    borderColor: CORES_CATEGORIAS[i % CORES_CATEGORIAS.length],
    backgroundColor: CORES_CATEGORIAS[i % CORES_CATEGORIAS.length] + '20',
    borderWidth: 2,
    pointRadius: 4,
    pointHoverRadius: 6,
    tension: 0.3,
    fill: false,
    hidden: !categoriasAtivas.has(s.categoria),
  }));

  lineChartInstance = new Chart(canvas, {
    type: 'line',
    data: {
      labels: serieData.periodos,
      datasets: datasets,
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false,
      },
      plugins: {
        legend: {
          display: false,
        },
        tooltip: {
          callbacks: {
            label: ctx => `${ctx.dataset.label}: ${ctx.parsed.y} unidades`,
          },
        },
      },
      scales: {
        x: {
          title: { display: true, text: 'Periodo' },
          grid: { color: '#f0f0f0' },
        },
        y: {
          title: { display: true, text: 'Quantidade Vendida' },
          beginAtZero: true,
          ticks: { stepSize: 1 },
          grid: { color: '#f0f0f0' },
        },
      },
    },
  });
}

function updateChartVisibility(serieData, categoriasAtivas) {
  if (!lineChartInstance) return;
  serieData.series.forEach((s, i) => {
    lineChartInstance.data.datasets[i].hidden = !categoriasAtivas.has(s.categoria);
  });
  lineChartInstance.update();
}

function aplicarMascaraData(input) {
  input.addEventListener('input', (e) => {
    let v = e.target.value.replace(/\D/g, '');
    if (v.length > 8) v = v.slice(0, 8);
    if (v.length > 4) v = v.slice(0, 2) + '/' + v.slice(2, 4) + '/' + v.slice(4);
    else if (v.length > 2) v = v.slice(0, 2) + '/' + v.slice(2);
    e.target.value = v;
  });
}

function converterParaISO(dataBR) {
  const partes = dataBR.split('/');
  if (partes.length !== 3 || partes[2].length !== 4) return null;
  const [dia, mes, ano] = partes;
  return `${ano}-${mes.padStart(2, '0')}-${dia.padStart(2, '0')}`;
}

export function initReportMasks() {
  const inicio = document.getElementById('report-data-inicio');
  const fim = document.getElementById('report-data-fim');
  if (inicio) aplicarMascaraData(inicio);
  if (fim) aplicarMascaraData(fim);
}

export async function handleLoadReport(e) {
  e.preventDefault();
  const dataInicioBR = document.getElementById('report-data-inicio').value;
  const dataFimBR = document.getElementById('report-data-fim').value;

  if (!dataInicioBR || !dataFimBR) {
    showToast("Selecione as datas de inicio e fim.", "error");
    return;
  }

  const dataInicio = converterParaISO(dataInicioBR);
  const dataFim = converterParaISO(dataFimBR);

  if (!dataInicio || !dataFim) {
    showToast("Formato invalido. Use dd/mm/aaaa.", "error");
    return;
  }

  const content = document.getElementById('report-content');
  content.innerHTML = '<div class="loading">Gerando relatorio...</div>';

  try {
    const [vendas, categorias, serieTemporal] = await Promise.all([
      apiRequest(`/api/relatorios/vendas?inicio=${dataInicio}&fim=${dataFim}`).catch(() => []),
      apiRequest(`/api/relatorios/vendas/categorias?inicio=${dataInicio}&fim=${dataFim}`).catch(() => []),
      apiRequest(`/api/relatorios/vendas/categorias/serie-temporal?inicio=${dataInicio}&fim=${dataFim}`).catch(() => null),
    ]);

    state.reportData = {
      vendas: Array.isArray(vendas) ? vendas : (vendas.content || []),
    };
    state.reportCategorias = Array.isArray(categorias) ? categorias : [];
    state.reportSerieTemporal = serieTemporal;

    content.innerHTML = renderReportContent(state.reportData, state.reportCategorias, serieTemporal);

    if (serieTemporal && serieTemporal.series && serieTemporal.series.length > 0) {
      renderLineChart(serieTemporal);
    } else {
      const chartContainer = document.getElementById('line-chart-categorias');
      if (chartContainer) {
        chartContainer.parentElement.innerHTML = '<div class="empty-state" style="padding: 2rem;"><i class="fas fa-chart-line"></i><p>Nenhum dado encontrado para o periodo selecionado</p></div>';
      }
    }
  } catch (err) {
    showToast("Erro ao gerar relatorio: " + err.message, "error");
    content.innerHTML = '<div class="empty-state"><i class="fas fa-exclamation-triangle"></i><h3>Erro ao gerar relatorio</h3></div>';
  }
}
