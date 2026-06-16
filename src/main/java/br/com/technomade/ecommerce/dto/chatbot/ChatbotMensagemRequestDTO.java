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
public class ChatbotMensagemRequestDTO {

    private String mensagem;
    private List<ChatbotMensagemHistoricoDTO> historico;
}
