package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.relatorio.ProdutoVendaDTO;
import br.com.technomade.ecommerce.service.RelatorioVendasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioVendasController {

    @Autowired
    private RelatorioVendasService relatorioVendasService;

    // relatorio de vendas por periodo
    @GetMapping("/vendas")
    public ResponseEntity<List<ProdutoVendaDTO>> relatorioVendas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
            ) {
        List<ProdutoVendaDTO> relatorio = relatorioVendasService.gerarRelatorioPorPeriodo(inicio, fim);
        return ResponseEntity.ok(relatorio);
    }
}
