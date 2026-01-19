package br.com.technomade.ecommerce.repository;

import br.com.technomade.ecommerce.model.ItemCarrinho;
import br.com.technomade.ecommerce.model.Produto;
import br.com.technomade.ecommerce.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ItemCarrinhoRepository extends JpaRepository<ItemCarrinho, Long> {
    List<ItemCarrinho> findAllByUsuario(Usuario usuario);

    Optional<ItemCarrinho> findByUsuarioAndProduto(Usuario usuario, Produto produto);

    List<ItemCarrinho> findAllByUsuarioAndReservadoAteBefore(Usuario usuario, LocalDateTime dataHora);

    List<ItemCarrinho> findAllByProdutoAndReservadoAteAfter(Produto produto, LocalDateTime data);
}
