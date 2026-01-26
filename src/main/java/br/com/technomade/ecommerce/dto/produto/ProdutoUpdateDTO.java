package br.com.technomade.ecommerce.dto.produto;

import lombok.Data;

import java.util.Set;

@Data
public class ProdutoUpdateDTO {
    private String nome;
    private String descricao;
    private String marca;
    private String imagemUrl;
    private Double altura;
    private Double largura;
    private Double profundidade;
    private Double peso;
    private Double valorCusto;
    private Double valorVenda;
    private String codigoBarras;
    private Set<Long> categoriaIds;
    private Long grupoPrecificacaoId;
    private Boolean ativo;
}
