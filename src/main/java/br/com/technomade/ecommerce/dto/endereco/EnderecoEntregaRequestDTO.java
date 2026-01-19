package br.com.technomade.ecommerce.dto.endereco;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EnderecoEntregaRequestDTO {

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
}
