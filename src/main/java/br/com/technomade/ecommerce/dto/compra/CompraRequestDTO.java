package br.com.technomade.ecommerce.dto.compra;

import lombok.Data;

import java.util.List;

import br.com.technomade.ecommerce.dto.endereco.EnderecoEntregaRequestDTO;

@Data
public class CompraRequestDTO {

    private Long enderecoEntregaId;
    private EnderecoEntregaRequestDTO novoEnderecoEntrega;
    private Boolean salvarEnderecoNoPerfil;
    private List<PagamentoRequestDTO> pagamentos;
}
