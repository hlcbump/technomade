package br.com.technomade.ecommerce.repository;

import br.com.technomade.ecommerce.model.AuditoriaLog;
import br.com.technomade.ecommerce.model.TipoOperacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Long> {

    // Buscar logs por entidade
    List<AuditoriaLog> findByEntidadeOrderByDataHoraDesc(String entidade);

    // Buscar logs de uma entidade específica
    List<AuditoriaLog> findByEntidadeAndEntidadeIdOrderByDataHoraDesc(String entidade, Long entidadeId);

    // Buscar logs por usuário
    List<AuditoriaLog> findByUsuarioIdOrderByDataHoraDesc(Long usuarioId);

    // Buscar logs por tipo de operação
    List<AuditoriaLog> findByOperacaoOrderByDataHoraDesc(TipoOperacao operacao);

    // Buscar logs por período
    @Query("SELECT a FROM AuditoriaLog a WHERE a.dataHora BETWEEN :inicio AND :fim ORDER BY a.dataHora DESC")
    List<AuditoriaLog> findByPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    // Buscar logs recentes (últimos N registros)
    List<AuditoriaLog> findTop100ByOrderByDataHoraDesc();
}
