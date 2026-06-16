package br.com.technomade.ecommerce.repository;

import br.com.technomade.ecommerce.model.Cliente;
import br.com.technomade.ecommerce.model.ItemCarrinho;
import br.com.technomade.ecommerce.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ItemCarrinhoRepository extends JpaRepository<ItemCarrinho, Long> {
    List<ItemCarrinho> findAllByCliente(Cliente cliente);

    Optional<ItemCarrinho> findByClienteAndProduto(Cliente cliente, Produto produto);

    List<ItemCarrinho> findAllByClienteAndReservadoAteBefore(Cliente cliente, LocalDateTime dataHora);

    List<ItemCarrinho> findAllByProdutoAndReservadoAteAfter(Produto produto, LocalDateTime data);
}
