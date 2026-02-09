package br.com.technomade.ecommerce.controller;
import br.com.technomade.ecommerce.dto.troca.PedidoTrocaRequest;
import br.com.technomade.ecommerce.model.ItemTroca;
import br.com.technomade.ecommerce.model.PedidoTroca;
import br.com.technomade.ecommerce.model.Produto;
import br.com.technomade.ecommerce.model.StatusTroca;
import br.com.technomade.ecommerce.repository.ProdutoRepository;
import br.com.technomade.ecommerce.service.PedidoTrocaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"http://localhost:8000"})
@RestController
@RequestMapping("/api/trocas")
public class PedidoTrocaController {

    @Autowired
    private PedidoTrocaService pedidoTrocaService;

    @Autowired
    private ProdutoRepository produtoRepository;

    // listar todas as trocas ou filtrar por status
    @GetMapping
    public ResponseEntity<List<PedidoTroca>> listarTrocas(
            @RequestParam(required = false) StatusTroca status
    ) {
        List<PedidoTroca> trocas;
        if (status != null) {
            trocas = pedidoTrocaService.listarPorStatus(status);
        } else {
            trocas = pedidoTrocaService.listarTodas();
        }
        return ResponseEntity.ok(trocas);
    }

    // buscar trocas por id
    @GetMapping("/{id}")
    public ResponseEntity<PedidoTroca> buscarPorId(@PathVariable Long id) {
        PedidoTroca troca = pedidoTrocaService.buscarPorId(id);
        return ResponseEntity.ok(troca);
    }

    // listar trocas de um cliente especifico
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<PedidoTroca>> listarPorCliente(@PathVariable Long clienteId) {
        List<PedidoTroca> trocas = pedidoTrocaService.listarPorCliente(clienteId);
        return ResponseEntity.ok(trocas);
    }

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

    // confirmar recebimento do produto da troca e reestocar
    @PutMapping("/{id}/receber")
    public ResponseEntity<Void> confirmarRecebimento(@PathVariable Long id, boolean reestocar){
        pedidoTrocaService.confirmarRecebimento(id, reestocar);
        return ResponseEntity.ok().build();
    }
}