package br.com.technomade.ecommerce.dto.usuario;

import br.com.technomade.ecommerce.model.Genero;
import br.com.technomade.ecommerce.model.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

// dados do usuário retornados pelo sistema após consulta
@Data
@Builder
public class UsuarioResponseDTO {

    private Long id;

    private String nome;
    private Genero genero;
    private LocalDate dataNascimento;

    // e-mail cadastrado
    private String email;

    // tipo ADMIN ou CLIENTE
    private Role role;

    private String cpf;
    private String telefone;

    // endereço principal
    private String endereco;
    private boolean ativo;
}
