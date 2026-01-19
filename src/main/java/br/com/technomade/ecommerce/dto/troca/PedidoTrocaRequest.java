package br.com.technomade.ecommerce.dto.troca;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PedidoTrocaRequest {

    @NotNull
    private Long compraId;

    @NotNull
    private List<ItemTrocaRequestDTO> itens;
}
