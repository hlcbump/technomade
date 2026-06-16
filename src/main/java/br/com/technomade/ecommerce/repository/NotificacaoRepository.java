package br.com.technomade.ecommerce.repository;

import br.com.technomade.ecommerce.model.Notificacao;
import br.com.technomade.ecommerce.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    // Buscar notificações de um usuário, ordenadas por data (mais recentes primeiro)
    List<Notificacao> findByUsuarioOrderByDataHoraDesc(Usuario usuario);

    // Buscar notificações não lidas de um usuário
    List<Notificacao> findByUsuarioAndLidaFalseOrderByDataHoraDesc(Usuario usuario);

    // Contar notificações não lidas de um usuário
    Long countByUsuarioAndLidaFalse(Usuario usuario);

    // Buscar notificações por tipo
    List<Notificacao> findByUsuarioAndTipoOrderByDataHoraDesc(Usuario usuario, br.com.technomade.ecommerce.model.TipoNotificacao tipo);

    // Buscar notificações por referência
    @Query("SELECT n FROM Notificacao n WHERE n.usuario = :usuario AND n.referenciaId = :referenciaId AND n.referenciaTipo = :referenciaTipo ORDER BY n.dataHora DESC")
    List<Notificacao> findByUsuarioAndReferencia(
            @Param("usuario") Usuario usuario,
            @Param("referenciaId") Long referenciaId,
            @Param("referenciaTipo") String referenciaTipo
    );
}
