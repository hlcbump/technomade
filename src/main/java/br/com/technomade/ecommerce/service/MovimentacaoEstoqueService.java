package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.model.Estoque;
import br.com.technomade.ecommerce.model.MovimentacaoEstoque;
import br.com.technomade.ecommerce.model.TipoMovimentacao;
import br.com.technomade.ecommerce.repository.EstoqueRepository;
import br.com.technomade.ecommerce.repository.MovimentacaoEstoqueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimentacaoEstoqueService {

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private ProdutoService produtoService;

    public List<MovimentacaoEstoque> listarTodos(){
        return movimentacaoEstoqueRepository.findAll();
    }

    @Transactional
    public MovimentacaoEstoque salvar(MovimentacaoEstoque movimentacaoEstoque){
        validarMovimentacao(movimentacaoEstoque);
        movimentacaoEstoque.setDataHora(LocalDateTime.now());

        Estoque estoque = estoqueRepository.findByProdutoId(movimentacaoEstoque.getProduto().getId())
                .orElseGet(() -> {
                    if (movimentacaoEstoque.getTipo() == TipoMovimentacao.SAIDA){
                        throw new IllegalStateException("Não existe estoque para saída do produto informado");
                    }
                    return new Estoque(
                            null,
                            movimentacaoEstoque.getProduto(),
                            movimentacaoEstoque.getValorCusto(),
                            0,
                            movimentacaoEstoque.getFornecedor(),
                            null
                    );
                });

        if(movimentacaoEstoque.getTipo() == TipoMovimentacao.ENTRADA){
            Double valorAtual = estoque.getValorCusto() == null ? 0.0 : estoque.getValorCusto();
            Double valorEntrada = movimentacaoEstoque.getValorCusto();
            Double valorFinal = Math.max(valorAtual, valorEntrada);

            estoque.setValorCusto(valorFinal);
            estoque.setQuantidade(estoque.getQuantidade() + movimentacaoEstoque.getQuantidade());
            estoque.setFornecedor(movimentacaoEstoque.getFornecedor());
            estoque.setUltimaEntrada(LocalDateTime.now());

            produtoService.atualizarPrecificacao(estoque.getProduto(), valorFinal);
        } else if (movimentacaoEstoque.getTipo() == TipoMovimentacao.SAIDA){
            if (estoque.getQuantidade() < movimentacaoEstoque.getQuantidade()){
                throw new IllegalStateException("Quantidade insuficiente em estoque para saída");
            }
            estoque.setQuantidade(estoque.getQuantidade() - movimentacaoEstoque.getQuantidade());
        }

        estoqueRepository.save(estoque);
        return movimentacaoEstoqueRepository.save(movimentacaoEstoque);
    }

    private void validarMovimentacao(MovimentacaoEstoque movimentacaoEstoque){
        if (movimentacaoEstoque == null){
            throw new IllegalArgumentException("Movimentação inválida");
        }

        if (movimentacaoEstoque.getProduto() == null || movimentacaoEstoque.getProduto().getId() == null){
            throw new IllegalArgumentException("Produto é obrigatório");
        }

        if (movimentacaoEstoque.getTipo() == null){
            throw new IllegalArgumentException("Tipo de movimentação é obrigatório");
        }

        if (movimentacaoEstoque.getQuantidade() == null || movimentacaoEstoque.getQuantidade() <= 0){
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }

        if (movimentacaoEstoque.getTipo() == TipoMovimentacao.ENTRADA){
            if (movimentacaoEstoque.getValorCusto() == null || movimentacaoEstoque.getValorCusto() <= 0){
                throw new IllegalArgumentException("Valor de custo é obrigatório e deve ser maior que zero");
            }

            if (movimentacaoEstoque.getFornecedor() == null || movimentacaoEstoque.getFornecedor().trim().isEmpty()){
                throw new IllegalArgumentException("Fornecedor é obrigatório");
            }
        }
    }
}
