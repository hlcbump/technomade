package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.model.AuditoriaLog;
import br.com.technomade.ecommerce.service.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = {"http://localhost:8000"})
@RestController
@RequestMapping("/api/auditoria")
// apenas role admin pode acessar
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;

    // pegar os 100 logs mais recentes
    @GetMapping
    public ResponseEntity<List<AuditoriaLog>> listarRecentes() {
        List<AuditoriaLog> logs = auditoriaService.listarTodos();
        return ResponseEntity.ok(logs);
    }

    // buscar os logs por entidade
    @GetMapping("/entidade/{entidade}")
    public ResponseEntity<List<AuditoriaLog>> buscarPorEntidade(@PathVariable String entidade) {
        List<AuditoriaLog> logs = auditoriaService.buscarPorEntidade(entidade);
        return ResponseEntity.ok(logs);
    }

    // buscar logs de uma entidade especifica
    @GetMapping("/entidade/{entidade}/{id}")
    public ResponseEntity<List<AuditoriaLog>> buscarPorEntidadeId(
            @PathVariable String entidade,
            @PathVariable Long id
    ) {
        List<AuditoriaLog> logs = auditoriaService.buscarPorEntidadeId(entidade, id);
        return ResponseEntity.ok(logs);
    }

    // buscar logs por usuario
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<AuditoriaLog>> buscarPorUsuario(@PathVariable Long usuarioId) {
        List<AuditoriaLog> logs = auditoriaService.buscarPorUsuario(usuarioId);
        return ResponseEntity.ok(logs);
    }

    // buscar logs por periodo
    @GetMapping("/periodo")
    public ResponseEntity<List<AuditoriaLog>> buscarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim
    ) {
        List<AuditoriaLog> logs = auditoriaService.buscarPorPeriodo(inicio, fim);
        return ResponseEntity.ok(logs);
    }
}
