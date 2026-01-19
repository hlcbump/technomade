package br.com.technomade.ecommerce.dto.cartao;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartaoCreditoResponseDTO {

    private Long id;
    private String numero;
    private String nomeImpresso;
    private String bandeira;
    private boolean preferencial;
}
