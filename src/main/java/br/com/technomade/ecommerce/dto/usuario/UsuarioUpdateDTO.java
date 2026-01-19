package br.com.technomade.ecommerce.dto.usuario;

import lombok.Data;

@Data
public class UsuarioUpdateDTO {
    private String nome;
    private String telefone;
    private String endereco;
}
