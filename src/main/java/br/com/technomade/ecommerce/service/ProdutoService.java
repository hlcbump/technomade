package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.model.Produto;
import br.com.technomade.ecommerce.model.AcaoStatusProduto;
import br.com.technomade.ecommerce.model.MotivoAtivacao;
import br.com.technomade.ecommerce.model.MotivoInativacao;
import br.com.technomade.ecommerce.model.Categoria;
import br.com.technomade.ecommerce.model.GrupoPrecificacao;
import br.com.technomade.ecommerce.dto.produto.ProdutoUpdateDTO;
import br.com.technomade.ecommerce.repository.ProdutoRepository;
import br.com.technomade.ecommerce.repository.spec.ProdutoSpecifications;
import br.com.technomade.ecommerce.repository.CategoriaRepository;
import br.com.technomade.ecommerce.repository.GrupoPrecificacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private GrupoPrecificacaoRepository grupoPrecificacaoRepository;

    public Produto salvar(Produto produto){
        if (produto.getValorCusto() != null) {
            aplicarPrecificacao(produto, produto.getValorCusto());
        }
        return produtoRepository.save(produto);
    }

    public List<Produto> listarTodos(){
        return produtoRepository.findAll();
    }

    public List<Produto> listarComFiltros(
            String nome,
            String descricao,
            String marca,
            String codigoBarras,
            Long categoriaId,
            Long grupoPrecificacaoId,
            Boolean ativo,
            Double valorVendaMin,
            Double valorVendaMax,
            Double valorCustoMin,
            Double valorCustoMax
    ){
        return produtoRepository.findAll(ProdutoSpecifications.comFiltros(
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
        ));
    }

    public Optional<Produto> buscarPorId(Long id){
        return produtoRepository.findById(id);
    }

    public Produto atualizar(Long id, ProdutoUpdateDTO dto){
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));

        if (dto.getNome() != null) produto.setNome(dto.getNome());
        if (dto.getDescricao() != null) produto.setDescricao(dto.getDescricao());
        if (dto.getMarca() != null) produto.setMarca(dto.getMarca());
        if (dto.getImagemUrl() != null) produto.setImagemUrl(dto.getImagemUrl());
        if (dto.getAltura() != null) produto.setAltura(dto.getAltura());
        if (dto.getLargura() != null) produto.setLargura(dto.getLargura());
        if (dto.getProfundidade() != null) produto.setProfundidade(dto.getProfundidade());
        if (dto.getPeso() != null) produto.setPeso(dto.getPeso());
        if (dto.getCodigoBarras() != null) produto.setCodigoBarras(dto.getCodigoBarras());

        if (dto.getGrupoPrecificacaoId() != null){
            GrupoPrecificacao grupo = grupoPrecificacaoRepository.findById(dto.getGrupoPrecificacaoId())
                    .orElseThrow(() -> new IllegalArgumentException("Grupo de precificação não encontrado"));
            produto.setGrupoPrecificacao(grupo);
        }

        if (dto.getCategoriaIds() != null){
            Set<Categoria> categorias = dto.getCategoriaIds().stream()
                    .map(idCategoria -> categoriaRepository.findById(idCategoria)
                            .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + idCategoria)))
                    .collect(Collectors.toSet());
            produto.setCategorias(categorias);
        }

        boolean precificar = false;
        if (dto.getValorCusto() != null) {
            produto.setValorCusto(dto.getValorCusto());
            precificar = true;
        }

        if (dto.getValorVenda() != null) {
            produto.setValorVenda(dto.getValorVenda());
        }

        if (dto.getAtivo() != null) {
            produto.setAtivo(dto.getAtivo());
        }

        if (precificar) {
            aplicarPrecificacao(produto, produto.getValorCusto());
        }

        return produtoRepository.save(produto);
    }

    public void deletarPorId(Long id){
        produtoRepository.deleteById(id);
    }

    public Produto atualizarPrecificacao(Produto produto, Double valorCusto){
        aplicarPrecificacao(produto, valorCusto);
        return produtoRepository.save(produto);
    }

    public Produto inativar(Long id, MotivoInativacao motivo, String justificativa){
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));

        if (!produto.isAtivo()){
            throw new IllegalStateException("Produto já está inativo");
        }

        if (motivo == null){
            throw new IllegalArgumentException("motivo de inativação é obrigatório");
        }

        if (justificativa == null || justificativa.trim().isEmpty()){
            throw new IllegalArgumentException("Justificativa de inativação é obrigatoria");
        }

        produto.setAtivo(false);
        produto.setUltimaAcao(AcaoStatusProduto.INATIVACAO);
        produto.setUltimoMotivo(motivo.name());
        produto.setUltimaJustificativa(justificativa.trim());
        produto.setUltimaData(LocalDateTime.now());

        return produtoRepository.save(produto);
    }

    public Produto ativar(Long id, MotivoAtivacao motivo, String justificativa){
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));

        if (produto.isAtivo()){
            throw new IllegalStateException("Produto ja está ativo");
        }

        if (motivo == null){
            throw new IllegalArgumentException("Motivo de ativação é obrigatório");
        }

        if (justificativa == null || justificativa.trim().isEmpty()){
            throw new IllegalArgumentException("Justificativa de ativação é obrigatória");
        }

        produto.setAtivo(true);
        produto.setUltimaAcao(AcaoStatusProduto.ATIVACAO);
        produto.setUltimoMotivo(motivo.name());
        produto.setUltimaJustificativa(justificativa.trim());
        produto.setUltimaData(LocalDateTime.now());

        return produtoRepository.save(produto);
    }

    private void aplicarPrecificacao(Produto produto, Double valorCusto){
        if (produto.getGrupoPrecificacao() == null || produto.getGrupoPrecificacao().getMargemLucro() == null){
            throw new IllegalStateException("Grupo de precificação é obrigatório para calcular o valor de venda");
        }

        double margem = produto.getGrupoPrecificacao().getMargemLucro();
        double valorVenda = valorCusto + (valorCusto * margem);
        produto.setValorCusto(valorCusto);
        produto.setValorVenda(valorVenda);
    }
}
