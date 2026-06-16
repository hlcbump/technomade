package br.com.technomade.ecommerce.repository;

import br.com.technomade.ecommerce.model.Cliente;
import br.com.technomade.ecommerce.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    Optional<Cliente> findByUsuario(Usuario usuario);

    Optional<Cliente> findByUsuarioId(Long usuarioId);

    Optional<Cliente> findByUsuarioEmail(String email);
}
