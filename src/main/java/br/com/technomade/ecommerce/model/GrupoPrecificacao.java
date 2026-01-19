package br.com.technomade.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "grupo_precificacao")
@Builder
@EqualsAndHashCode(exclude = {"produtos"})
public class GrupoPrecificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    @NotNull
    @Column(name = "margem_lucro")
    private Double margemLucro;

    @OneToMany(mappedBy = "grupoPrecificacao")
    @JsonIgnore
    private Set<Produto> produtos;
}
