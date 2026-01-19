package br.com.technomade.ecommerce.dto.cartao;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CartaoCreditoRequestDTO {

    @NotBlank
    private String numero;

    @NotBlank
    private String nomeImpresso;

    @NotBlank
    private String bandeira;

    @NotBlank
    private String codigoSegurança;

    private boolean preferencial = false;
}
