package br.com.technomade.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "itens_troca")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemTroca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // muitos items de troca pertecem a um mesmo pedido de troca
    @ManyToOne
    @JoinColumn(name = "troca_id", nullable = false)
    private PedidoTroca troca;

    // muitos itens de troca podem ser o mesmo produto
    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @NotNull
    private Integer quantidade;
}
