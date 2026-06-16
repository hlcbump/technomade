package br.com.technomade.ecommerce.dto.relatorio;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoriaVendaDTO {
    private String nomeCategoria;
    private long totalQuantidade;
    private double totalValor;
}
