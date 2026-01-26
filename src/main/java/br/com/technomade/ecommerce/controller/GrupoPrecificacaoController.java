package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.model.GrupoPrecificacao;
import br.com.technomade.ecommerce.service.GrupoPrecificacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/gruposprecificacao")
public class GrupoPrecificacaoController {

    @Autowired
    private GrupoPrecificacaoService grupoPrecificacaoService;

    @GetMapping
    public List<GrupoPrecificacao> listar() {
        return grupoPrecificacaoService.listarTodos();
    }

    @GetMapping("/{id}")
    // pathvariable pega valores dinamicos da URL e passa como argumento no controller
    public Optional<GrupoPrecificacao> buscarPorId(@PathVariable Long id) {
        return grupoPrecificacaoService.buscarPorId(id);
    }

    @PostMapping
    // requestbody converte o json enviado pelo postman em um objeto
    public GrupoPrecificacao salvar(@RequestBody GrupoPrecificacao grupoPrecificacao){
        return grupoPrecificacaoService.salvar(grupoPrecificacao);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id){
        grupoPrecificacaoService.deletarPorId(id);
    }
}
