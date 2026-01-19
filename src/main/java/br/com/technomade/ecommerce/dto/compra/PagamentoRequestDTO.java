package br.com.technomade.ecommerce.dto.compra;

import lombok.Data;

@Data
public class PagamentoRequestDTO {

    private String formaPagamento;
    private Long cartaoCreditoId;
    private String cupomCodigo;
    private Double valor;

}
