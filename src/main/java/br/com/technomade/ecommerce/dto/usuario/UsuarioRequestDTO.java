package br.com.technomade.ecommerce.dto.usuario;

import br.com.technomade.ecommerce.model.Genero;
import br.com.technomade.ecommerce.model.Role;
import br.com.technomade.ecommerce.dto.endereco.EnderecoEntregaRequestDTO;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// dados enviados pelo cliente para criar uma nova conta
@Data
public class UsuarioRequestDTO {

    // nome completo do usuário 
    @NotBlank
    private String nome;

    // gênero do usuário 
    private Genero genero;

    // data de nascimento do usuário
    private LocalDate dataNascimento;

    // e-mail usado para login 
    @NotBlank
    @Email
    private String email;

    // senha da conta 
    @NotBlank
    private String senha;

    // tipo de acesso
    private Role role;

    // CPF do usuário
    private String cpf;

    // telefone de contato
    private String telefone;

    // endereço principal 
    private String endereco;

    // lista de endereços de entrega que o usuario quer cadastrar junto com a conta
    private List<EnderecoEntregaRequestDTO> enderecosEntrega;
}
