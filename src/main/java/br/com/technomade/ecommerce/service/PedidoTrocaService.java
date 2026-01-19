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

    // solicitar troca - cliente
    @Transactional
    public PedidoTroca solicitarTroca(Long compraId, List<ItemTroca> itensParaTroca){
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra não encontrada"));

        if (compra.getStatusCompra() != StatusCompra.ENTREGUE){
            throw new IllegalStateException("Trocas só são permitidas para compras com status ENTREGUE");
        }

        PedidoTroca pedidoTroca = PedidoTroca.builder()
                .compra(compra)
                .statusTroca(StatusTroca.EM_TROCA)
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
        return pedidoTrocaRepository.save(pedidoTroca);
    }

    // confirma recebimento e gera cupom
    @Transactional
    public void confirmarRecebimento(Long pedidoTrocaId, boolean reestocar){
        PedidoTroca troca = pedidoTrocaRepository.findById(pedidoTrocaId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de troca não encontrado"));

        troca.setStatusTroca(StatusTroca.TROCADA);
        troca.setDataConclusao(LocalDateTime.now());
        pedidoTrocaRepository.save(troca);

        if (reestocar){
            for (ItemTroca item : troca.getItens()){
                Produto produto = item.getProduto();
                Estoque estoque = estoqueRepository.findByProdutoId(produto.getId())
                        .orElseThrow(() -> new IllegalStateException("Estoque não encontrado para " + produto.getNome()));
                estoque.setQuantidade(estoque.getQuantidade() + item.getQuantidade());
                estoque.setUltimaEntrada(LocalDateTime.now());
                estoqueRepository.save(estoque);
            }
        }

        Cupom cupomTroca = Cupom.builder()
                .codigo("TROCA-" + troca.getId() + "-" + System.currentTimeMillis())
                .valor(calcularValorTotal(troca))
                .promocional(false)
                .usado(false)
                .validade(LocalDateTime.now().plusMonths(3))
                .cliente(troca.getCompra().getCliente())
                .build();

        cupomRepository.save(cupomTroca);
    }

    private double calcularValorTotal(PedidoTroca pedidoTroca){
        return  pedidoTroca.getItens().stream()
                .mapToDouble(item -> item.getProduto().getValorVenda() * item.getQuantidade())
                .sum();
    }
}
