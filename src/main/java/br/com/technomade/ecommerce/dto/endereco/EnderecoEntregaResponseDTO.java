package br.com.technomade.ecommerce.dto.endereco;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnderecoEntregaResponseDTO {

    private Long id;

    private String nomeEndereco;

    private String tipoResidencia;

    private String tipoLogradouro;

    private String logradouro;

    private String numero;

    private String bairro;

    private String cep;

    private String cidade;

    private String estado;

    private String pais;

    private String observacoes;
}
