package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.model.Estoque;
import br.com.technomade.ecommerce.model.MotivoInativacao;
import br.com.technomade.ecommerce.model.Produto;
import br.com.technomade.ecommerce.repository.EstoqueRepository;
import br.com.technomade.ecommerce.repository.ItemCompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProdutoInativacaoAutomaticaService {

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private ItemCompraRepository itemCompraRepository;

    @Autowired
    private ProdutoService produtoService;

    private static final double VALOR_MINIMO_VENDAS = 100.0;
    private static final long INTERVALO_MS = 3600000L;

    @Scheduled(fixedDelay = INTERVALO_MS)
    public void executar(){
        List<Estoque> estoquesZerados = estoqueRepository.findByQuantidadeLessThanEqual(0);

        for (Estoque estoque : estoquesZerados){
            Produto produto = estoque.getProduto();
            if (produto == null || !produto.isAtivo()){
                continue;
            }

            Double totalVendas = itemCompraRepository.sumTotalVendidoPorProduto(produto.getId());
            if (totalVendas == null){
                totalVendas = 0.0;
            }

            if (totalVendas < VALOR_MINIMO_VENDAS){
                produtoService.inativar(
                        produto.getId(),
                        MotivoInativacao.FORA_DE_MERCADO,
                        "Inativação automática: estoque zerado e vendas abaixo do mínimo (" + VALOR_MINIMO_VENDAS + ")"
                );
            }
        }
    }
}
