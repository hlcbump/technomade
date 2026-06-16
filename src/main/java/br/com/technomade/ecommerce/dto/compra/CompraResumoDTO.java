package br.com.technomade.ecommerce.dto.compra;

import br.com.technomade.ecommerce.model.StatusCompra;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CompraResumoDTO {
    private Long id;
    private StatusCompra statusCompra;
    private Double valorTotal;
    private LocalDateTime dataCompra;
}
