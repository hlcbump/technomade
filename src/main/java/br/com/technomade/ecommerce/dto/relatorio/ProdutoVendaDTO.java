package br.com.technomade.ecommerce.dto.relatorio;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProdutoVendaDTO {
    private String nomeProduto;
    private Long quantidadeVendida;
    private Double valorTotal;
}
