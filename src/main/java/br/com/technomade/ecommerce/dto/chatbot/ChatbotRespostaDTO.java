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
public class ChatbotRespostaDTO {

    private String resposta;
    private List<ChatbotProdutoDTO> produtosReferenciados;
}
