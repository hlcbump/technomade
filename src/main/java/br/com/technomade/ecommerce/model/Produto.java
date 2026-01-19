package br.com.technomade.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "produtos")
@EqualsAndHashCode(exclude = {"categorias", "grupoPrecificacao"})
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    @NotBlank
    private String descricao;

    @NotBlank
    private String marca;

    @NotBlank
    private String imagemUrl; //talvez devo alterar dps

    @NotNull
    private Double altura;

    @NotNull
    private Double largura;

    @NotNull
    private Double profundidade;

    @NotNull
    private Double peso;

    @NotNull
    @Column(name = "valor_custo")
    private Double valorCusto;

    @NotNull
    @Column(name = "valor_venda")
    private Double valorVenda;

    @NotBlank
    @Column(name = "codigo_barras", unique = true)
    private String codigoBarras;

    @ManyToMany
    @JoinTable(
            name = "produto_categoria",
            joinColumns = @JoinColumn(name = "produto_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    private Set<Categoria> categorias;

    @ManyToOne
    @JoinColumn(name = "grupo_precificacao_id")
    private GrupoPrecificacao grupoPrecificacao;

    private boolean ativo = true;
}
