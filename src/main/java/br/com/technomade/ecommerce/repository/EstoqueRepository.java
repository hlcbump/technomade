package br.com.technomade.ecommerce.repository;

import br.com.technomade.ecommerce.model.Estoque;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {
    Optional<Estoque> findByProdutoId(Long produtoId);

    // lock pessimista para evitar race condition na baixa de estoque
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Estoque e WHERE e.produto.id = :produtoId")
    Optional<Estoque> findByProdutoIdComLock(@Param("produtoId") Long produtoId);

    List<Estoque> findByQuantidadeLessThanEqual(Integer quantidade);
}
