package br.com.technomade.ecommerce.controller;
import br.com.technomade.ecommerce.dto.troca.PedidoTrocaRequest;
import br.com.technomade.ecommerce.model.ItemTroca;
import br.com.technomade.ecommerce.model.PedidoTroca;
import br.com.technomade.ecommerce.model.Produto;
import br.com.technomade.ecommerce.repository.ProdutoRepository;
import br.com.technomade.ecommerce.service.PedidoTrocaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trocas")
public class PedidoTrocaController {

    @Autowired
    private PedidoTrocaService pedidoTrocaService;

    @Autowired
    private ProdutoRepository produtoRepository;

    // endpoint solicitar troca - cliente
    @PostMapping
    public ResponseEntity<PedidoTroca> solicitarTroca(@RequestBody PedidoTrocaRequest dto){
        List<ItemTroca> itens = dto.getItens().stream()
                .map(itemDto -> {
                    Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                            .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));
                    return ItemTroca.builder()
                            .produto(produto)
                            .quantidade(itemDto.getQuantidade())
                            .build();
                })
                .collect(Collectors.toList());

        PedidoTroca troca = pedidoTrocaService.solicitarTroca(dto.getCompraId(), itens);
        return ResponseEntity.ok(troca);
    }

    // endpoint para autorizar troca - admin
    @PutMapping("/{id}/autorizar")
    public ResponseEntity<PedidoTroca> autorizarTroca(@PathVariable Long id){
        PedidoTroca troca = pedidoTrocaService.autorizarTroca(id);
        return ResponseEntity.ok(troca);
    }

    @PutMapping("/{id}/receber")
    public ResponseEntity<Void> confirmarRecebimento(@PathVariable Long id, boolean reestocar){
        pedidoTrocaService.confirmarRecebimento(id, reestocar);
        return ResponseEntity.ok().build();
    }
}
