package br.com.technomade.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "enderecos_entrega")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnderecoEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RN0021/RN0022 - Tipo do endereco (ENTREGA, COBRANCA, AMBOS)
    @NotBlank
    @Builder.Default
    private String tipoEndereco = "ENTREGA";

    @NotBlank
    private String nomeEndereco;

    @NotBlank
    private String tipoResidencia;

    @NotBlank
    private String tipoLogradouro;

    @NotBlank
    private String logradouro;

    @NotBlank
    private String numero;

    @NotBlank
    private String bairro;

    @NotBlank
    private String cep;

    @NotBlank
    private String cidade;

    @NotBlank
    private String estado;

    @NotBlank
    private String pais;

    private String observacoes;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    @JsonIgnore
    private Cliente cliente;

}
