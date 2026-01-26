package br.com.technomade.ecommerce.dto.produto;

import br.com.technomade.ecommerce.model.MotivoAtivacao;
import lombok.Data;

@Data
public class AtivarProdutoRequestDTO {
    private MotivoAtivacao motivo;
    private String justificativa;
}
