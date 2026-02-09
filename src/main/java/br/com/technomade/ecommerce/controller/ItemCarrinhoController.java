package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.carrinho.ItemCarrinhoRequestDTO;
import br.com.technomade.ecommerce.dto.carrinho.ItemCarrinhoResponseDTO;
import br.com.technomade.ecommerce.dto.carrinho.ItemCarrinhoUpdateDTO;
import br.com.technomade.ecommerce.service.ItemCarrinhoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:8000"})
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

    // retorna minutos restantes atee expiração do carrinho
    @GetMapping("/tempo-restante")
    public ResponseEntity<Map<String, Long>> calcularTempoRestante() {
        long minutos = itemCarrinhoService.calcularMinutosRestantes();
        return ResponseEntity.ok(Map.of("minutosRestantes", minutos));
    }

    // notifica usuario se carrinho está prestes a expirar / front chama
    @PostMapping("/verificar-expiracao")
    public ResponseEntity<Map<String, Boolean>> verificarExpiracao() {
        itemCarrinhoService.notificarSeProximoExpiracao();
        boolean deveNotificar = itemCarrinhoService.deveNotificarExpiracao();
        return ResponseEntity.ok(Map.of("proximoExpiracao", deveNotificar));
    }
}
