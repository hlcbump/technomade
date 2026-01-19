package br.com.technomade.ecommerce.dto.troca;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemTrocaRequestDTO {

    @NotNull
    private Long produtoId;

    private int quantidade;
}
