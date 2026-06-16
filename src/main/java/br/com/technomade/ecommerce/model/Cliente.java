package br.com.technomade.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"enderecosEntrega", "cartaoCreditos"})
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    private String nome;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Genero genero;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(unique = true)
    private String cpf;

    private String telefone;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<EnderecoEntrega> enderecosEntrega;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CartaoCredito> cartaoCreditos;
}
