package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.model.*;
import br.com.technomade.ecommerce.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoTrocaService {

    @Autowired
    private PedidoTrocaRepository pedidoTrocaRepository;

    @Autowired
    private ItemTrocaRepository itemTrocaRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private CupomRepository cupomRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    // solicitar troca - cliente
    @Transactional
    public PedidoTroca solicitarTroca(Long compraId, List<ItemTroca> itensParaTroca, String motivo){
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra não encontrada"));

        if (compra.getStatusCompra() != StatusCompra.ENTREGUE){
            throw new IllegalStateException("Trocas só são permitidas para compras com status ENTREGUE");
        }

        PedidoTroca pedidoTroca = PedidoTroca.builder()
                .compra(compra)
                .statusTroca(StatusTroca.EM_TROCA)
                .motivo(motivo)
                .dataSolicitacao(LocalDateTime.now())
                .build();

        PedidoTroca trocaSalva = pedidoTrocaRepository.save(pedidoTroca);

        itensParaTroca.forEach(item -> item.setTroca(trocaSalva));
        itemTrocaRepository.saveAll(itensParaTroca);

        // atualizar o status da compra original
        compra.setStatusCompra(StatusCompra.EM_TROCA);
        compraRepository.save(compra);

        return trocaSalva;
    }

    // admin autoriza a troca
    @Transactional
    public PedidoTroca autorizarTroca(Long pedidoTrocaId){
        PedidoTroca pedidoTroca = pedidoTrocaRepository.findById(pedidoTrocaId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de troca não encontrado"));

        pedidoTroca.setStatusTroca(StatusTroca.TROCA_AUTORIZADA);
        PedidoTroca trocaSalva = pedidoTrocaRepository.save(pedidoTroca);

        // atualizar status da compra para TROCA_AUTORIZADA
        Compra compra = trocaSalva.getCompra();
        compra.setStatusCompra(StatusCompra.TROCA_AUTORIZADA);
        compraRepository.save(compra);

        // Notificar cliente sobre autorização da troca
        notificacaoService.notificarTrocaAutorizada(
            trocaSalva.getCompra().getCliente().getUsuario(),
            trocaSalva.getId()
        );

        return trocaSalva;
    }

    // admin nega a troca
    @Transactional
    public PedidoTroca negarTroca(Long pedidoTrocaId, String motivo){
        PedidoTroca pedidoTroca = pedidoTrocaRepository.findById(pedidoTrocaId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de troca não encontrado"));

        if (pedidoTroca.getStatusTroca() != StatusTroca.EM_TROCA) {
            throw new IllegalStateException("Apenas trocas com status EM_TROCA podem ser negadas");
        }

        pedidoTroca.setStatusTroca(StatusTroca.TROCA_RECUSADA);
        pedidoTroca.setDataConclusao(LocalDateTime.now());
        PedidoTroca trocaSalva = pedidoTrocaRepository.save(pedidoTroca);

        // volta status da compra para ENTREGUE
        Compra compra = trocaSalva.getCompra();
        compra.setStatusCompra(StatusCompra.TROCA_RECUSADA);
        compraRepository.save(compra);

        return trocaSalva;
    }

    // confirma recebimento e gera cupom
    @Transactional
    public void confirmarRecebimento(Long pedidoTrocaId, boolean reestocar){
        PedidoTroca troca = pedidoTrocaRepository.findById(pedidoTrocaId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de troca não encontrado"));

        troca.setStatusTroca(StatusTroca.TROCADA);
        troca.setDataConclusao(LocalDateTime.now());
        pedidoTrocaRepository.save(troca);

        // atualizar status da compra para TROCADA
        Compra compra = troca.getCompra();
        compra.setStatusCompra(StatusCompra.TROCADA);
        compraRepository.save(compra);

        if (reestocar){
            for (ItemTroca item : troca.getItens()){
                Produto produto = item.getProduto();
                Estoque estoque = estoqueRepository.findByProdutoId(produto.getId())
                        .orElseThrow(() -> new IllegalStateException("Estoque não encontrado para " + produto.getNome()));

                MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                        .produto(produto)
                        .tipo(TipoMovimentacao.ENTRADA)
                        .quantidade(item.getQuantidade())
                        .valorCusto(estoque.getValorCusto() != null ? estoque.getValorCusto() : produto.getValorVenda())
                        .fornecedor("Devolução - Troca #" + troca.getId())
                        .build();

                movimentacaoEstoqueService.salvar(movimentacao);
            }
        }

        // gera cupom apenas se o valor da troca for maior que zero
        // (compras pagas integralmente com cupom resultam em valor 0)
        double valorCupom = calcularValorTotal(troca);
        if (valorCupom > 0) {
            Cupom cupomTroca = Cupom.builder()
                    .codigo("TROCA-" + troca.getId() + "-" + System.currentTimeMillis())
                    .valor(valorCupom)
                    .promocional(false)
                    .usado(false)
                    .validade(LocalDateTime.now().plusMonths(3))
                    .cliente(troca.getCompra().getCliente())
                    .build();

            cupomRepository.save(cupomTroca);

            // Notificar cliente sobre cupom gerado
            notificacaoService.notificarCupomGerado(
                troca.getCompra().getCliente().getUsuario(),
                cupomTroca.getCodigo(),
                cupomTroca.getValor()
            );
        }
    }

    private double calcularValorTotal(PedidoTroca pedidoTroca){
        Compra compra = pedidoTroca.getCompra();

        // subtotal original dos itens da compra (sem desconto)
        double subtotalCompra = compra.getItens().stream()
                .mapToDouble(item -> item.getPrecoUnitario() * item.getQuantidade())
                .sum();

        // proporção que o cliente efetivamente pagou (descontando cupons)
        double proporcaoPaga = subtotalCompra > 0 ? compra.getValorTotal() / subtotalCompra : 1.0;

        // valor dos itens trocados com a mesma proporção de desconto
        double valorItensTroca = pedidoTroca.getItens().stream()
                .mapToDouble(itemTroca -> {
                    // buscar o preço unitário que foi pago na compra original
                    return compra.getItens().stream()
                            .filter(ic -> ic.getProduto().getId().equals(itemTroca.getProduto().getId()))
                            .findFirst()
                            .map(ic -> ic.getPrecoUnitario() * itemTroca.getQuantidade())
                            .orElse(itemTroca.getProduto().getValorVenda() * itemTroca.getQuantidade());
                })
                .sum();

        return Math.round(valorItensTroca * proporcaoPaga * 100.0) / 100.0;
    }

    /**
     * Lista todas as trocas
     */
    public List<PedidoTroca> listarTodas() {
        return pedidoTrocaRepository.findAll();
    }

    /**
     * Lista trocas por status
     */
    public List<PedidoTroca> listarPorStatus(StatusTroca status) {
        return pedidoTrocaRepository.findByStatusTroca(status);
    }

    /**
     * Busca troca por ID
     */
    public PedidoTroca buscarPorId(Long id) {
        return pedidoTrocaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de troca não encontrado"));
    }

    /**
     * Lista trocas de um cliente específico
     */
    public List<PedidoTroca> listarPorCliente(Long clienteId) {
        return pedidoTrocaRepository.findByCompraClienteId(clienteId);
    }
}
