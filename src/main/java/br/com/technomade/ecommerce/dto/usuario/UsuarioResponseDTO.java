package br.com.technomade.ecommerce.dto.usuario;

import br.com.technomade.ecommerce.model.Genero;
import br.com.technomade.ecommerce.model.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UsuarioResponseDTO {
    private Long id;
    private String nome;
    private Genero genero;
    private LocalDate dataNascimento;
    private String email;
    private Role role;
    private String cpf;
    private String telefone;
    private String endereco;
    private boolean ativo;
}
