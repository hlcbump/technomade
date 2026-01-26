package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.produto.AtivarProdutoRequestDTO;
import br.com.technomade.ecommerce.dto.produto.InativarProdutoRequestDTO;
import br.com.technomade.ecommerce.dto.produto.ProdutoUpdateDTO;
import br.com.technomade.ecommerce.model.Produto;
import br.com.technomade.ecommerce.service.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/produtos") // prefixo para todas as rotas
public class ProdutoController {

    @Autowired
    private ProdutoService produtoService;

    @GetMapping
    public List<Produto> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String codigoBarras,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long grupoPrecificacaoId,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Double valorVendaMin,
            @RequestParam(required = false) Double valorVendaMax,
            @RequestParam(required = false) Double valorCustoMin,
            @RequestParam(required = false) Double valorCustoMax
    ) {
        return produtoService.listarComFiltros(
                nome,
                descricao,
                marca,
                codigoBarras,
                categoriaId,
                grupoPrecificacaoId,
                ativo,
                valorVendaMin,
                valorVendaMax,
                valorCustoMin,
                valorCustoMax
        );
    }

    @GetMapping("/{id}")
    // pathvariable pega valores dinamicos da URL e passa como argumento no controller
    public Optional<Produto> buscarPorId(@PathVariable Long id) {
        return produtoService.buscarPorId(id);
    }

    @PostMapping
    // requestbody converte o json enviado pelo postman em um objeto
    public Produto salvar(@RequestBody Produto produto) {
        return produtoService.salvar(produto);
    }

    @PutMapping("/{id}")
    public Produto atualizar(@PathVariable Long id, @RequestBody ProdutoUpdateDTO dto){
        return produtoService.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        produtoService.deletarPorId(id);
    }

    @PutMapping("/{id}/inativar")
    public Produto inativar(@PathVariable Long id, @RequestBody InativarProdutoRequestDTO dto){
        return produtoService.inativar(id, dto.getMotivo(), dto.getJustificativa());
    }

    @PutMapping("/{id}/ativar")
    public Produto ativar(@PathVariable Long id, @RequestBody AtivarProdutoRequestDTO dto){
        return produtoService.ativar(id, dto.getMotivo(), dto.getJustificativa());
    }
}
