package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.model.*;
import br.com.technomade.ecommerce.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    @Transactional
    public Compra finalizarCompra(Usuario usuario, EnderecoEntrega enderecoEntrega, List<Pagamento> pagamentos) {
        List<ItemCarrinho> itensCarrinho = itemCarrinhoRepository.findAllByUsuario(usuario);

        if (itensCarrinho.isEmpty()){
            throw new IllegalStateException("Carrinho está vazio");
        }

        // calcular valor total da compra
        Double valorTotal = itensCarrinho.stream()
                .mapToDouble(item -> item.getProduto().getValorVenda() * item.getQuantidade())
                .sum();

        // criar a compra
        Compra compra = Compra.builder()
                .cliente(usuario)
                .enderecoEntrega(enderecoEntrega)
                .statusCompra(StatusCompra.EM_PROCESSAMENTO)
                .valorTotal(valorTotal)
                .dataCompra(LocalDateTime.now())
                .build();

        final Compra compraSalva = compraRepository.save(compra);

        // cria os itens da compra
        List<ItemCompra> itensCompra = itensCarrinho.stream()
                .map(item -> ItemCompra.builder()
                        .compra(compraSalva)
                        .produto(item.getProduto())
                        .quantidade(item.getQuantidade())
                        .precoUnitario(item.getProduto().getValorVenda())
                        .build())
                .collect(Collectors.toList());
        itemCompraRepository.saveAll(itensCompra);

        // validar cupons antes de salvar os pagamentos
        pagamentos.forEach(p ->{
            if(p.getCupom() != null){
                Cupom cupomValido = cupomService.validarCupom(p.getCupom().getCodigo());
                p.setCupom(cupomValido);
            }
        });

        // registrar os pagamentos recebidos
        pagamentos.forEach(p -> {
            p.setCompra(compraSalva);
            p.setDataPagamento(LocalDateTime.now());
        });
        pagamentoRepository.saveAll(pagamentos);

        // marcar cupons como usados
        pagamentos.stream()
                .filter(p -> p.getCupom() != null)
                .forEach(p -> cupomService.marcarComoUsado(p.getCupom()));

        // da baixa no estoque
        for (ItemCarrinho item : itensCarrinho){
            Produto produto = item.getProduto();
            Estoque estoque = estoqueRepository.findByProdutoId(produto.getId())
                    .orElseThrow(() -> new IllegalStateException("Estoque não encontrado, produto id:" + produto.getId()));
            if (estoque.getQuantidade() < item.getQuantidade()) {
                throw new IllegalStateException("Estoque insuficiente, produto:" + produto.getNome());
            }

            estoque.setQuantidade(estoque.getQuantidade() - item.getQuantidade());
            estoqueRepository.save(estoque);
        }

        // limpar o carrinho
        itemCarrinhoRepository.deleteAll(itensCarrinho);

        return compraSalva;
    }

    @Transactional
    public Compra atualizarStatus(Long compraId, StatusCompra novoStatus){
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra não encontrada"));

        if (novoStatus == StatusCompra.EM_TRANSITO && compra.getStatusCompra() != StatusCompra.APROVADA) {
            throw new IllegalStateException("Apenas compras aprovadas podem ser despachadas");
        }

        if (novoStatus == StatusCompra.ENTREGUE && compra.getStatusCompra() != StatusCompra.EM_TRANSITO){
            throw new IllegalStateException("Apenas compras em transito podem ser marcadas como entregue");
        }

        compra.setStatusCompra(novoStatus);
        return compraRepository.save(compra);
    }

}
