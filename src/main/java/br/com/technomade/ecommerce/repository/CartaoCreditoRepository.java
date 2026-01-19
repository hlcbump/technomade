package br.com.technomade.ecommerce.repository;

import br.com.technomade.ecommerce.model.CartaoCredito;
import br.com.technomade.ecommerce.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartaoCreditoRepository extends JpaRepository<CartaoCredito, Long> {
    List<CartaoCredito> findAllByUsuario(Usuario usuario);

    Optional<CartaoCredito> findByUsuarioAndPreferencialTrue(Usuario usuario);
}