package br.com.technomade.ecommerce.dto.troca;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PedidoTrocaResponseDTO {

    private Long id;
    private String status;
    private LocalDateTime dataSolicitacao;
    private LocalDateTime dataConclusao;
    private List<ItemTrocaResponseDTO> itens;
}
