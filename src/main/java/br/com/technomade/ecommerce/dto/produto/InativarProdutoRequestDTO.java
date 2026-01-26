package br.com.technomade.ecommerce.dto.produto;

import br.com.technomade.ecommerce.model.MotivoInativacao;
import lombok.Data;

@Data
public class InativarProdutoRequestDTO {
    private MotivoInativacao motivo;
    private String justificativa;
}
