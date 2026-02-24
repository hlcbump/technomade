package br.com.technomade.ecommerce.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// dados enviados pelo usuário quando quer trocar a senha
@Data
public class AlterarSenhaRequestDTO {

    // senha atual da conta, para confirmar a identidade (obrigatório)
    @NotBlank
    private String senhaAtual;

    // nova senha (obrigatório)
    @NotBlank
    private String novaSenha;
}
