package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.model.MovimentacaoEstoque;
import br.com.technomade.ecommerce.service.MovimentacaoEstoqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/movimentacoes")
public class MovimentacaoEstoqueController {

    @Autowired
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @GetMapping
    public List<MovimentacaoEstoque> listar(){
        return movimentacaoEstoqueService.listarTodos();
    }

    @PostMapping
    public MovimentacaoEstoque salvar(@RequestBody MovimentacaoEstoque movimentacaoEstoque){
        return movimentacaoEstoqueService.salvar(movimentacaoEstoque);
    }

}
