package br.com.technomade.ecommerce.dto.usuario;

import lombok.Data;

// dados que o usuário pode alterar no seu perfil
@Data
public class UsuarioUpdateDTO {

    private String nome;
    private String telefone;
    private String endereco;
}
