package br.com.technomade.ecommerce.dto.carrinho;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ItemCarrinhoResponseDTO {
    private Long id;
    private Long produtoId;
    private String nomeProduto;
    private Integer quantidade;
    private LocalDateTime reservadoAte;
}
