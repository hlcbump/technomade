package br.com.technomade.ecommerce.dto.usuario;

import br.com.technomade.ecommerce.model.Genero;
import lombok.Data;

import java.time.LocalDate;

// dados que o usuário pode alterar no seu perfil
@Data
public class UsuarioUpdateDTO {

    private String nome;
    private String email;
    private Genero genero;
    private LocalDate dataNascimento;
    private String cpf;
    private String telefone;
}
