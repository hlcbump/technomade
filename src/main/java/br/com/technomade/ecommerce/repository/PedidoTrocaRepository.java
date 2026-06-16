package br.com.technomade.ecommerce.repository;

import br.com.technomade.ecommerce.model.PedidoTroca;
import br.com.technomade.ecommerce.model.StatusTroca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoTrocaRepository extends JpaRepository<PedidoTroca, Long> {

    // Buscar trocas por status
    List<PedidoTroca> findByStatusTroca(StatusTroca status);

    // Buscar trocas por cliente
    List<PedidoTroca> findByCompraClienteId(Long clienteId);
}
