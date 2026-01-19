package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.model.Estoque;
import br.com.technomade.ecommerce.service.EstoqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {

    @Autowired
    private EstoqueService estoqueService;

    @GetMapping
    public List<Estoque> listar(){
        return estoqueService.listarTodos();
    }

    @GetMapping("/{id}")
    public Optional<Estoque> buscarPorId(@PathVariable Long id){
        return estoqueService.buscarPorId(id);
    }

    @GetMapping("/produto/{produtoId}")
    public Optional<Estoque> buscarPorProduto(@PathVariable Long produtoId){
        return estoqueService.buscarPorProdutoId(produtoId);
    }

    @PostMapping
    public Estoque salvar(@RequestBody Estoque estoque){
        return estoqueService.salvar(estoque);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id){
        estoqueService.deletarPorId(id);
    }
}
