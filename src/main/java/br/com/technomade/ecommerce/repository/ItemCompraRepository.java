package br.com.technomade.ecommerce.repository;

import br.com.technomade.ecommerce.model.ItemCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemCompraRepository extends JpaRepository<ItemCompra, Long> {

    // busca itens de compra em um intervalo
    List<ItemCompra> findByCompra_DataCompraBetween(LocalDateTime inicio, LocalDateTime fim);
}
