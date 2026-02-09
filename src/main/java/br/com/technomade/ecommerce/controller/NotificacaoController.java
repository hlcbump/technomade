package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.notificacao.NotificacaoResponseDTO;
import br.com.technomade.ecommerce.service.NotificacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:8000"})
@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    @Autowired
    private NotificacaoService notificacaoService;


    // listar todas as notificações do usuaro logado
    @GetMapping
    public ResponseEntity<List<NotificacaoResponseDTO>> listarMinhas() {
        List<NotificacaoResponseDTO> notificacoes = notificacaoService.listarMinhasNotificacoes();
        return ResponseEntity.ok(notificacoes);
    }

    // lista notificacões não lidas
    @GetMapping("/nao-lidas")
    public ResponseEntity<List<NotificacaoResponseDTO>> listarNaoLidas() {
        List<NotificacaoResponseDTO> notificacoes = notificacaoService.listarNaoLidas();
        return ResponseEntity.ok(notificacoes);
    }


    // conta notificacões não lidas
    @GetMapping("/nao-lidas/count")
    public ResponseEntity<Map<String, Long>> contarNaoLidas() {
        Long count = notificacaoService.contarNaoLidas();
        return ResponseEntity.ok(Map.of("count", count));
    }

    // busca notificacao por id
    @GetMapping("/{id}")
    public ResponseEntity<NotificacaoResponseDTO> buscarPorId(@PathVariable Long id) {
        NotificacaoResponseDTO notificacao = notificacaoService.buscarPorId(id);
        return ResponseEntity.ok(notificacao);
    }

    // marca notf especifica como lida
    @PutMapping("/{id}/ler")
    public ResponseEntity<NotificacaoResponseDTO> marcarComoLida(@PathVariable Long id) {
        NotificacaoResponseDTO notificacao = notificacaoService.marcarComoLida(id);
        return ResponseEntity.ok(notificacao);
    }

    // marca todas notf como lida
    @PutMapping("/ler-todas")
    public ResponseEntity<Map<String, String>> marcarTodasComoLidas() {
        notificacaoService.marcarTodasComoLidas();
        return ResponseEntity.ok(Map.of("message", "Todas as notificacões foram marcadas como lidas"));
    }
}
