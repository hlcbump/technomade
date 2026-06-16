package br.com.technomade.ecommerce.dto.compra;

import lombok.Data;

import br.com.technomade.ecommerce.dto.cartao.CartaoCreditoRequestDTO;

@Data
public class PagamentoRequestDTO {

    private String formaPagamento;
    private Long cartaoCreditoId;
    private CartaoCreditoRequestDTO novoCartao;
    private Boolean salvarCartaoNoPerfil;
    private String cupomCodigo;
    private Double valor;

}
