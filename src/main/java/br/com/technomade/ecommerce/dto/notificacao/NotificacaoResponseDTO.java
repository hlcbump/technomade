package br.com.technomade.ecommerce.dto.notificacao;

import br.com.technomade.ecommerce.model.TipoNotificacao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoResponseDTO {

    private Long id;
    private TipoNotificacao tipo;
    private String mensagem;
    private Boolean lida;
    private LocalDateTime dataHora;

    // Referências opcionais
    private Long referenciaId;
    private String referenciaTipo;
}
