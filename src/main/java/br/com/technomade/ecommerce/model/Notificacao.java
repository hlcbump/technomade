package br.com.technomade.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacao tipo;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(nullable = false)
    @Builder.Default
    private Boolean lida = false;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    // Campos opcionais para referências
    @Column(name = "referencia_id")
    private Long referenciaId; // ID da compra, troca, produto, etc

    @Column(name = "referencia_tipo", length = 50)
    private String referenciaTipo; // "COMPRA", "TROCA", "PRODUTO", etc

    @PrePersist
    public void prePersist() {
        if (dataHora == null) {
            dataHora = LocalDateTime.now();
        }
        if (lida == null) {
            lida = false;
        }
    }
}
