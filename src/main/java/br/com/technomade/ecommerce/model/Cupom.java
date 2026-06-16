package br.com.technomade.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cupom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String codigo;

    @NotNull
    private Double valor;

    private boolean promocional;

    private boolean usado;

    private LocalDateTime validade;

    // muitos cupons podem estar associado a um mesmo cliente (null para promocionais globais)
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
}
