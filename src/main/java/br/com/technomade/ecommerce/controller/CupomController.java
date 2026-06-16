package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.model.Cliente;
import br.com.technomade.ecommerce.model.Cupom;
import br.com.technomade.ecommerce.repository.CupomRepository;
import br.com.technomade.ecommerce.service.CupomService;
import br.com.technomade.ecommerce.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cupons")
public class CupomController {

    @Autowired
    private CupomRepository cupomRepository;

    @Autowired
    private CupomService cupomService;

    @Autowired
    private UsuarioService usuarioService;

    // listar todos os cupons (admin)
    @GetMapping
    public ResponseEntity<List<Cupom>> listarTodos() {
        return ResponseEntity.ok(cupomRepository.findAll());
    }

    // listar cupons de um cliente pelo usuarioId
    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<Cupom>> listarCuponsDoUsuario(@PathVariable Long usuarioId) {
        Cliente cliente = usuarioService.buscarClientePorUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        List<Cupom> cupons = cupomRepository.findAll().stream()
                .filter(c -> c.getCliente() != null && c.getCliente().getId().equals(cliente.getId()))
                .toList();
        return ResponseEntity.ok(cupons);
    }

    // validar cupom por codigo
    @GetMapping("/validar/{codigo}")
    public ResponseEntity<Cupom> validarCupom(@PathVariable String codigo) {
        Cupom cupom = cupomService.validarCupom(codigo);
        return ResponseEntity.ok(cupom);
    }

    // criar cupom promocional (admin)
    @PostMapping
    public ResponseEntity<Cupom> criarCupomPromocional(@RequestBody Map<String, Object> body) {
        String codigo = (String) body.get("codigo");
        Double valor = body.get("valor") instanceof Number ? ((Number) body.get("valor")).doubleValue() : null;
        Integer validadeDias = body.get("validadeDias") instanceof Number ? ((Number) body.get("validadeDias")).intValue() : 30;

        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Codigo do cupom e obrigatorio");
        }
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("Valor do cupom deve ser positivo");
        }

        Boolean isPromocional = body.get("promocional") instanceof Boolean ? (Boolean) body.get("promocional") : true;

        Long clienteId = body.get("clienteId") instanceof Number ? ((Number) body.get("clienteId")).longValue() : null;
        Cliente cliente = null;
        if (clienteId != null) {
            cliente = usuarioService.buscarClientePorUsuarioId(clienteId)
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para o usuário informado"));
        }

        Cupom cupom = Cupom.builder()
                .codigo(codigo.toUpperCase())
                .valor(valor)
                .promocional(isPromocional)
                .usado(false)
                .validade(LocalDateTime.now().plusDays(validadeDias))
                .cliente(cliente)
                .build();

        return ResponseEntity.ok(cupomRepository.save(cupom));
    }
}
