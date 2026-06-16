package br.com.technomade.ecommerce.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotProdutoDTO {

    private Long id;
    private String nome;
    private String descricao;
    private String marca;
    private String imagemUrl;
    private Double valorVenda;
    private List<String> categorias;
}
