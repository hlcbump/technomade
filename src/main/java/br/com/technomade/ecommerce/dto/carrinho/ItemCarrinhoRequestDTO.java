package br.com.technomade.ecommerce.dto.carrinho;

import lombok.Data;

@Data
public class ItemCarrinhoRequestDTO {
    private Long produtoId;
    private Integer quantidade;
}
