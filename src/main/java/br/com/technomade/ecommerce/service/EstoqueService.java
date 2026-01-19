package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.model.Estoque;
import br.com.technomade.ecommerce.repository.EstoqueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EstoqueService {

    @Autowired
    private EstoqueRepository estoqueRepository;

    public Estoque salvar(Estoque estoque){
        return estoqueRepository.save(estoque);
    }

    public List<Estoque> listarTodos(){
        return estoqueRepository.findAll();
    }

    public Optional<Estoque> buscarPorId(Long id){
        return estoqueRepository.findById(id);
    }

    public Optional<Estoque> buscarPorProdutoId(Long id){
        return estoqueRepository.findByProdutoId(id);
    }

    public void deletarPorId(Long id){
        estoqueRepository.deleteById(id);
    }
}
