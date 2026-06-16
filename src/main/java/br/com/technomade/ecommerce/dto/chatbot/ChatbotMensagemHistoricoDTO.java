package br.com.technomade.ecommerce.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotMensagemHistoricoDTO {

    private String papel; // "usuario" ou "assistente"
    private String conteudo;
}
