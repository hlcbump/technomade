package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.chatbot.ChatbotMensagemRequestDTO;
import br.com.technomade.ecommerce.dto.chatbot.ChatbotRespostaDTO;
import br.com.technomade.ecommerce.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/mensagem")
    public ChatbotRespostaDTO enviarMensagem(@RequestBody ChatbotMensagemRequestDTO request) {
        return chatbotService.responder(request);
    }
}
