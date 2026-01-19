package br.com.technomade.ecommerce.dto.usuario;

import br.com.technomade.ecommerce.model.Genero;
import br.com.technomade.ecommerce.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UsuarioRequestDTO {

    @NotBlank
    private String nome;

    private Genero genero;

    private LocalDate dataNascimento;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String senha;

    private Role role;

    private String cpf;

    private String telefone;

    private String endereco;
}
