package br.com.technomade.ecommerce.dto.relatorio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerieTemporalCategoriaDTO {

    private long totalCompras;
    private List<String> periodos;
    private List<SerieCategoriaDTO> series;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SerieCategoriaDTO {
        private String categoria;
        private List<Long> quantidades;
    }
}
