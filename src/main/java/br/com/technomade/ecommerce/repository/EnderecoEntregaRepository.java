package br.com.technomade.ecommerce.repository;

import br.com.technomade.ecommerce.model.Cliente;
import br.com.technomade.ecommerce.model.EnderecoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnderecoEntregaRepository extends JpaRepository<EnderecoEntrega, Long> {
    List<EnderecoEntrega> findAllByCliente(Cliente cliente);
}
