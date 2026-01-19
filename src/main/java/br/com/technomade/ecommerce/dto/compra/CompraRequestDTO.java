package br.com.technomade.ecommerce.dto.compra;

import lombok.Data;

import java.util.List;

@Data
public class CompraRequestDTO {

    private Long usuarioId;
    private Long enderecoEntregaId;
    private List<PagamentoRequestDTO> pagamentos;
}
