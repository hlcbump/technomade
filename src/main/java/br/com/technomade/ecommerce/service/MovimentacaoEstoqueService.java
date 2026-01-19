package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.model.Estoque;
import br.com.technomade.ecommerce.model.MovimentacaoEstoque;
import br.com.technomade.ecommerce.model.TipoMovimentacao;
import br.com.technomade.ecommerce.repository.EstoqueRepository;
import br.com.technomade.ecommerce.repository.MovimentacaoEstoqueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimentacaoEstoqueService {

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    public List<MovimentacaoEstoque> listarTodos(){
        return movimentacaoEstoqueRepository.findAll();
    }

    public MovimentacaoEstoque salvar(MovimentacaoEstoque movimentacaoEstoque){
        movimentacaoEstoque.setDataHora(LocalDateTime.now());

        Estoque estoque = estoqueRepository.findByProdutoId(movimentacaoEstoque.getProduto().getId())
                .orElse(new Estoque(null, movimentacaoEstoque.getProduto(), 0, movimentacaoEstoque.getFornecedor(), null));

        if(movimentacaoEstoque.getTipo() == TipoMovimentacao.ENTRADA){
            estoque.setQuantidade(estoque.getQuantidade() + movimentacaoEstoque.getQuantidade());
            estoque.setFornecedor(movimentacaoEstoque.getFornecedor());
            estoque.setUltimaEntrada(LocalDateTime.now());
        } else if (movimentacaoEstoque.getTipo() == TipoMovimentacao.SAIDA){
            estoque.setQuantidade(estoque.getQuantidade() - movimentacaoEstoque.getQuantidade());
        }

        estoqueRepository.save(estoque);
        return movimentacaoEstoqueRepository.save(movimentacaoEstoque);
    }
}
