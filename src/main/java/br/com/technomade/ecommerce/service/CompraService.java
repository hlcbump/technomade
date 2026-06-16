package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.model.*;
import br.com.technomade.ecommerce.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CompraService {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ItemCarrinhoRepository itemCarrinhoRepository;

    @Autowired
    private ItemCompraRepository itemCompraRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private CupomService cupomService;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<Compra> listarTodas() {
        return compraRepository.findAll();
    }

    public List<Compra> listarPorCliente(Long clienteId) {
        return compraRepository.findByClienteId(clienteId);
    }

    @Transactional
    public Compra finalizarCompra(Cliente cliente, EnderecoEntrega enderecoEntrega, List<Pagamento> pagamentos) {
        List<ItemCarrinho> itensCarrinho = itemCarrinhoRepository.findAllByCliente(cliente);

        if (itensCarrinho.isEmpty()) {
            throw new IllegalStateException("Carrinho está vazio");
        }

        validarPagamentos(pagamentos);
        validarEstoque(itensCarrinho);

        double valorItens = calcularValorTotal(itensCarrinho);

        // RN0036 - Resolver cupons e validar uso desnecessário
        List<Pagamento> pagamentosCupom = resolverCupons(pagamentos);
        validarUsoDesnecessarioDeCupons(pagamentosCupom, valorItens);

        // Calcular o desconto real dos cupons (limitado ao valor dos itens)
        double totalCupons = pagamentosCupom.stream()
                .mapToDouble(p -> p.getCupom().getValor())
                .sum();

        double descontoEfetivo = Math.min(totalCupons, valorItens);
        double valorSemFrete = Math.max(valorItens - descontoEfetivo, 0);

        // Calcular frete — RN0047: grátis para SP
        double frete = calcularFreteInterno(itensCarrinho, enderecoEntrega);
        double valorTotal = valorSemFrete + frete;

        // Validar que a soma dos pagamentos cobre o valor total
        validarSomaPagamentos(pagamentos, valorTotal, pagamentosCupom);

        Compra compraSalva = salvarCompra(cliente, enderecoEntrega, valorTotal, frete);
        salvarItensCompra(compraSalva, itensCarrinho);

        // RN0036 - Ajustar valores dos pagamentos com cupom e gerar troco se necessário
        ajustarPagamentosCupomEGerarTroco(cliente, pagamentosCupom, valorItens);

        salvarPagamentos(compraSalva, pagamentos);
        itemCarrinhoRepository.deleteAll(itensCarrinho);

        return compraSalva;
    }

    @Transactional
    public Compra atualizarStatus(Long compraId, StatusCompra novoStatus) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra não encontrada"));

        StatusCompra statusAnterior = compra.getStatusCompra();

        validarTransicaoDeStatus(compra, novoStatus);

        if (novoStatus == StatusCompra.APROVADA) {
            darBaixaEstoque(compra);
        }

        compra.setStatusCompra(novoStatus);
        Compra compraSalva = compraRepository.save(compra);

        auditarMudancaDeStatus(compraSalva, statusAnterior, novoStatus);
        notificarCliente(compraSalva.getCliente().getUsuario(), compraSalva.getId(), novoStatus);

        return compraSalva;
    }

    public double calcularFrete(Cliente cliente, EnderecoEntrega enderecoEntrega) {
        List<ItemCarrinho> itensCarrinho = itemCarrinhoRepository.findAllByCliente(cliente);

        if (itensCarrinho.isEmpty()) {
            throw new IllegalStateException("Carrinho está vazio");
        }

        return calcularFreteInterno(itensCarrinho, enderecoEntrega);
    }

    // calcula frete a partir dos itens e endereço — RN0047: grátis para SP
    private double calcularFreteInterno(List<ItemCarrinho> itensCarrinho, EnderecoEntrega enderecoEntrega) {
        // RN0047 - frete grátis para entregas em São Paulo
        if (enderecoEntrega.getEstado() != null
                && enderecoEntrega.getEstado().toUpperCase().equals("SP")) {
            return 0.0;
        }

        double pesoTotal = itensCarrinho.stream()
                .mapToDouble(item -> item.getProduto().getPeso() * item.getQuantidade())
                .sum();

        double freteMínimo = 10.0;
        double frete = freteMínimo + (pesoTotal * 2.0);

        return Math.max(frete, freteMínimo);
    }

    private void validarPagamentos(List<Pagamento> pagamentos) {
        // RN0033 - apenas um cupom promocional pode ser utilizado por compra
        long qtdCuponsPromocionais = pagamentos.stream()
                .filter(p -> p.getFormaPagamento() == FormaPagamento.CUPOM_PROMOCIONAL)
                .count();
        if (qtdCuponsPromocionais > 1) {
            throw new IllegalArgumentException("Apenas um cupom promocional pode ser utilizado por compra (RN0033)");
        }

        // RN0034 + RN0035 - Valor mínimo de R$10,00 por cartão,
        // exceto quando combinado com cupons (permite valor menor)
        boolean temCupom = pagamentos.stream()
                .anyMatch(p -> p.getFormaPagamento() == FormaPagamento.CUPOM_PROMOCIONAL
                        || p.getFormaPagamento() == FormaPagamento.CUPOM_TROCA);

        for (Pagamento p : pagamentos) {
            if (p.getFormaPagamento() == FormaPagamento.CARTAO_CREDITO
                    && p.getValor() != null && p.getValor() < 10.0 && !temCupom) {
                throw new IllegalArgumentException("O valor mínimo para pagamento com cartão de crédito é R$ 10,00 (RN0034)");
            }
        }
    }

    // valida que a soma dos pagamentos com cartão cobre o valor restante após cupons
    // (valorTotal já tem os cupons descontados: valorItens - descontoEfetivo + frete)
    private void validarSomaPagamentos(List<Pagamento> pagamentos, double valorTotal, List<Pagamento> pagamentosCupom) {
        double totalCartoes = pagamentos.stream()
                .filter(p -> p.getFormaPagamento() == FormaPagamento.CARTAO_CREDITO)
                .mapToDouble(p -> p.getValor() != null ? p.getValor() : 0)
                .sum();

        // tolerância de 1 centavo para evitar erros de arredondamento
        if (valorTotal > 0 && totalCartoes < valorTotal - 0.01) {
            throw new IllegalArgumentException(
                    String.format("A soma dos cartões (R$ %.2f) não cobre o valor a pagar (R$ %.2f)",
                            totalCartoes, valorTotal));
        }
    }

    private void validarEstoque(List<ItemCarrinho> itensCarrinho) {
        for (ItemCarrinho item : itensCarrinho) {
            Produto produto = item.getProduto();
            Estoque estoque = estoqueRepository.findByProdutoId(produto.getId())
                    .orElseThrow(() -> new IllegalStateException("Estoque não encontrado, produto id:" + produto.getId()));
            if (estoque.getQuantidade() < item.getQuantidade()) {
                throw new IllegalStateException("Estoque insuficiente, produto:" + produto.getNome());
            }
        }
    }

    private double calcularValorTotal(List<ItemCarrinho> itensCarrinho) {
        return itensCarrinho.stream()
                .mapToDouble(item -> item.getProduto().getValorVenda() * item.getQuantidade())
                .sum();
    }

    private Compra salvarCompra(Cliente cliente, EnderecoEntrega enderecoEntrega, double valorTotal, double frete) {
        Compra compra = Compra.builder()
                .cliente(cliente)
                .enderecoEntrega(enderecoEntrega)
                .statusCompra(StatusCompra.EM_PROCESSAMENTO)
                .valorTotal(valorTotal)
                .frete(frete)
                .dataCompra(LocalDateTime.now())
                .dataEntregaPrevista(calcularDataEntregaPrevista(enderecoEntrega))
                .build();
        return compraRepository.save(compra);
    }

    private void salvarItensCompra(Compra compra, List<ItemCarrinho> itensCarrinho) {
        List<ItemCompra> itensCompra = itensCarrinho.stream()
                .map(item -> ItemCompra.builder()
                        .compra(compra)
                        .produto(item.getProduto())
                        .quantidade(item.getQuantidade())
                        .precoUnitario(item.getProduto().getValorVenda())
                        .build())
                .toList();
        itemCompraRepository.saveAll(itensCompra);
    }

    private void salvarPagamentos(Compra compra, List<Pagamento> pagamentos) {
        for (Pagamento p : pagamentos) {
            // Cupons já foram resolvidos em resolverCupons(), não re-validar
            p.setCompra(compra);
            p.setDataPagamento(LocalDateTime.now());
        }
        pagamentoRepository.saveAll(pagamentos);

        pagamentos.stream()
                .filter(p -> p.getCupom() != null)
                .forEach(p -> cupomService.marcarComoUsado(p.getCupom()));
    }

    // --- Helpers de atualizarStatus ---

    private void validarTransicaoDeStatus(Compra compra, StatusCompra novoStatus) {
        if (novoStatus == StatusCompra.EM_TRANSITO && compra.getStatusCompra() != StatusCompra.APROVADA) {
            throw new IllegalStateException("Apenas compras aprovadas podem ser despachadas");
        }
        if (novoStatus == StatusCompra.ENTREGUE && compra.getStatusCompra() != StatusCompra.EM_TRANSITO) {
            throw new IllegalStateException("Apenas compras em transito podem ser marcadas como entregue");
        }
    }

    private void auditarMudancaDeStatus(Compra compra, StatusCompra statusAnterior, StatusCompra novoStatus) {
        TipoOperacao tipoOperacao = switch (novoStatus) {
            case APROVADA  -> TipoOperacao.APROVACAO;
            case REPROVADA -> TipoOperacao.REPROVACAO;
            default        -> TipoOperacao.ATUALIZACAO;
        };

        auditoriaService.registrar(
                "Compra", compra.getId(), tipoOperacao,
                "Status: " + statusAnterior, "Status: " + novoStatus,
                String.format("Status da compra alterado de %s para %s", statusAnterior, novoStatus));
    }

    private void notificarCliente(Usuario usuario, Long compraId, StatusCompra novoStatus) {
        switch (novoStatus) {
            case APROVADA  -> notificacaoService.notificarCompraAprovada(usuario, compraId);
            case REPROVADA -> notificacaoService.notificarCompraReprovada(usuario, compraId);
            case EM_TRANSITO -> notificacaoService.notificarCompraEmTransito(usuario, compraId);
            case ENTREGUE  -> notificacaoService.notificarCompraEntregue(usuario, compraId);
            default        -> { /* outros status não geram notificação */ }
        }
    }

    // --- Helpers de estoque ---

    // --- Helpers RN0036: Cupons com troco ---

    /**
     * Resolve os cupons dos pagamentos antecipadamente para ter acesso aos valores reais.
     */
    private List<Pagamento> resolverCupons(List<Pagamento> pagamentos) {
        List<Pagamento> pagamentosCupom = new ArrayList<>();
        for (Pagamento p : pagamentos) {
            if (p.getCupom() != null && p.getCupom().getCodigo() != null) {
                Cupom cupomValido = cupomService.validarCupom(p.getCupom().getCodigo());
                p.setCupom(cupomValido);
                pagamentosCupom.add(p);
            }
        }
        return pagamentosCupom;
    }

    /**
     * RN0036 - Valida que não há uso desnecessário de cupons.
     * Se remover qualquer cupom individual e a soma dos restantes ainda cobre o valor,
     * então aquele cupom é desnecessário e o sistema deve rejeitar a combinação.
     */
    private void validarUsoDesnecessarioDeCupons(List<Pagamento> pagamentosCupom, double valorItens) {
        if (pagamentosCupom.size() <= 1) {
            return;
        }

        double totalCupons = pagamentosCupom.stream()
                .mapToDouble(p -> p.getCupom().getValor())
                .sum();

        // Se a soma dos cupons não excede o valor, todos são necessários
        if (totalCupons <= valorItens) {
            return;
        }

        // Verificar se algum cupom individual pode ser removido sem deficit
        for (Pagamento cupomTestado : pagamentosCupom) {
            double somaSemEste = totalCupons - cupomTestado.getCupom().getValor();
            if (somaSemEste >= valorItens) {
                throw new IllegalArgumentException(
                        "Uso desnecessário de cupons (RN0036). O cupom '"
                                + cupomTestado.getCupom().getCodigo()
                                + "' (R$ " + String.format("%.2f", cupomTestado.getCupom().getValor())
                                + ") é desnecessário, pois os demais cupons já cobrem o valor da compra."
                                + " Remova cupons excedentes para prosseguir.");
            }
        }
    }

    /**
     * RN0036 - Ajusta o valor do pagamento com cupom ao valor efetivamente utilizado.
     * Se o total dos cupons excede o valor dos itens, gera um cupom de troco com a diferença.
     */
    private void ajustarPagamentosCupomEGerarTroco(Cliente cliente, List<Pagamento> pagamentosCupom, double valorItens) {
        if (pagamentosCupom.isEmpty()) {
            return;
        }

        double valorRestante = valorItens;

        for (int i = 0; i < pagamentosCupom.size(); i++) {
            Pagamento p = pagamentosCupom.get(i);
            double valorCupom = p.getCupom().getValor();

            if (valorCupom <= valorRestante) {
                // Cupom é consumido integralmente
                p.setValor(valorCupom);
                valorRestante -= valorCupom;
            } else {
                // Cupom excede o valor restante — usar apenas o necessário e gerar troco
                p.setValor(valorRestante);
                double troco = valorCupom - valorRestante;
                valorRestante = 0;

                Cupom cupomTroco = cupomService.gerarCupomTroco(cliente, troco);
                notificacaoService.notificarCupomGerado(
                        cliente.getUsuario(), cupomTroco.getCodigo(), cupomTroco.getValor());
            }
        }
    }

    // SP: 5 dias úteis, outros estados: 10 dias úteis
    private LocalDate calcularDataEntregaPrevista(EnderecoEntrega endereco) {
        int diasUteis = "SP".equalsIgnoreCase(endereco.getEstado()) ? 5 : 10;
        LocalDate data = LocalDate.now();
        int contagem = 0;
        while (contagem < diasUteis) {
            data = data.plusDays(1);
            if (data.getDayOfWeek() != DayOfWeek.SATURDAY && data.getDayOfWeek() != DayOfWeek.SUNDAY) {
                contagem++;
            }
        }
        return data;
    }

    // usa lock pessimista (SELECT FOR UPDATE) para evitar race condition
    // entre duas aprovações simultâneas do mesmo produto
    private void darBaixaEstoque(Compra compra) {
        List<ItemCompra> itens = itemCompraRepository.findByCompraId(compra.getId());
        for (ItemCompra item : itens) {
            Produto produto = item.getProduto();
            Estoque estoque = estoqueRepository.findByProdutoIdComLock(produto.getId())
                    .orElseThrow(() -> new IllegalStateException("Estoque não encontrado, produto id:" + produto.getId()));
            if (estoque.getQuantidade() < item.getQuantidade()) {
                throw new IllegalStateException("Estoque insuficiente, produto:" + produto.getNome());
            }
            estoque.setQuantidade(estoque.getQuantidade() - item.getQuantidade());
            estoqueRepository.save(estoque);
        }
    }

}
