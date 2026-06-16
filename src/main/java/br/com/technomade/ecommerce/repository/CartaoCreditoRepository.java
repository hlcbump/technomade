package br.com.technomade.ecommerce.repository;

import br.com.technomade.ecommerce.model.CartaoCredito;
import br.com.technomade.ecommerce.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartaoCreditoRepository extends JpaRepository<CartaoCredito, Long> {
    List<CartaoCredito> findAllByCliente(Cliente cliente);

    Optional<CartaoCredito> findByClienteAndPreferencialTrue(Cliente cliente);

    boolean existsByNumero(String numero);
}
