package br.com.technomade.ecommerce.repository;

import br.com.technomade.ecommerce.model.Cupom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CupomRepository extends JpaRepository<Cupom, Long> {

    // buscar pelo codigo do cupom
    Optional<Cupom> findByCodigo(String codigo);

    // verificar se ja foi usado
    boolean existsByCodigoAndUsadoTrue(String codigo);
}
