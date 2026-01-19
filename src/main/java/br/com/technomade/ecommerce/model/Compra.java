package br.com.technomade.ecommerce.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "compras")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // muitos pedidos podem ser feitos por um mesmo usuario
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario cliente;

    // uma compra pode ter varios itens
    @OneToMany (mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCompra> itens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCompra statusCompra;

    @Column(name = "valor_total", nullable = false)
    private Double valorTotal;

    @Column(name = "data_compra", nullable = false)
    private LocalDateTime dataCompra;

    // varias comprar podem ter o mesmo endereço
    @ManyToOne
    @JoinColumn(name = "endereco_entrega_id", nullable = false)
    private EnderecoEntrega enderecoEntrega;
}
