package br.com.technomade.ecommerce.dto.cartao;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CartaoCreditoUpdateDTO {

    @NotBlank String nomeImpresso;

    private boolean preferencial;
}
