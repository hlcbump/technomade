package br.com.technomade.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "itens_compra")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // muitos itens pertecem a uma mesma compra
    @ManyToOne
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    // muitos itens podem ser o mesmo produto
    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @NotNull
    private Integer quantidade;

    @NotNull
    @Column(name = "preco_unitario")
    private Double precoUnitario;
}
