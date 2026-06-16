package br.com.technomade.ecommerce.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    // muitos pedidos podem ser feitos por um mesmo cliente
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // uma compra pode ter varios itens
    @OneToMany (mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCompra> itens;

    // uma compra pode ter varios pagamentos
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pagamento> pagamentos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCompra statusCompra;

    @Column(name = "valor_total", nullable = false)
    private Double valorTotal;

    @Column(name = "frete")
    private Double frete;

    @Column(name = "data_compra", nullable = false)
    private LocalDateTime dataCompra;

    @Column(name = "data_entrega_prevista")
    private LocalDate dataEntregaPrevista;

    // varias comprar podem ter o mesmo endereço
    @ManyToOne
    @JoinColumn(name = "endereco_entrega_id", nullable = false)
    private EnderecoEntrega enderecoEntrega;
}
