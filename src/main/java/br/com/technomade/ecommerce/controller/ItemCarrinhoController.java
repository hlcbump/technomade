package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.carrinho.ItemCarrinhoRequestDTO;
import br.com.technomade.ecommerce.dto.carrinho.ItemCarrinhoResponseDTO;
import br.com.technomade.ecommerce.dto.carrinho.ItemCarrinhoUpdateDTO;
import br.com.technomade.ecommerce.service.ItemCarrinhoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carrinho")
public class ItemCarrinhoController {

    @Autowired
    private ItemCarrinhoService itemCarrinhoService;

    @GetMapping
    public List<ItemCarrinhoResponseDTO> listar(){
        return itemCarrinhoService.listarDoUsuario();
    }

    @PostMapping
    public ItemCarrinhoResponseDTO adicionar(@RequestBody ItemCarrinhoRequestDTO dto){
        return itemCarrinhoService.adicionar(dto);
    }

    @PutMapping("/{id}")
    public ItemCarrinhoResponseDTO atualizarQuantidade(@PathVariable Long id, @RequestBody ItemCarrinhoUpdateDTO dto) {
        return itemCarrinhoService.atualizarQuantidade(id, dto);
    }

    @DeleteMapping("/{id}")
    public void remover(@PathVariable Long id){
        itemCarrinhoService.remover(id);
    }
}
