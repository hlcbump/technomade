package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.model.Cupom;
import br.com.technomade.ecommerce.repository.CupomRepository;
import br.com.technomade.ecommerce.service.CupomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cupons")
public class CupomController {

    @Autowired
    private CupomRepository cupomRepository;

    @Autowired
    private CupomService cupomService;

    // listar todos cupons de um cliente
    @GetMapping("/{id}")
    public ResponseEntity<List<Cupom>> listarCuponsDoUsuario(@PathVariable Long usuarioId){
        List<Cupom> cupons = cupomRepository.findAll().stream()
                .filter(c -> c.getCliente().getId().equals(usuarioId))
                .toList();
        return  ResponseEntity.ok(cupons);
    }

    // validar cupom por código
    @GetMapping("/validar/{codigo}")
    public  ResponseEntity<Cupom> validarCupom(@PathVariable String codigo){
        Cupom cupom  = cupomService.validarCupom(codigo);
        return ResponseEntity.ok(cupom);
    }
}
