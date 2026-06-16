package br.com.technomade.ecommerce.repository;

import br.com.technomade.ecommerce.model.ItemCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemCompraRepository extends JpaRepository<ItemCompra, Long> {

    // busca itens de compra em um intervalo
    List<ItemCompra> findByCompra_DataCompraBetween(LocalDateTime inicio, LocalDateTime fim);

    @Query("select coalesce(sum(i.precoUnitario * i.quantidade), 0) from ItemCompra i where i.produto.id = :produtoId")
    Double sumTotalVendidoPorProduto(@Param("produtoId") Long produtoId);

    List<ItemCompra> findByCompraId(Long compraId);
}
