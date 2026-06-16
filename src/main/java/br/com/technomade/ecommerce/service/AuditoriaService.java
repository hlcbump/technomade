package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.model.AuditoriaLog;
import br.com.technomade.ecommerce.model.TipoOperacao;
import br.com.technomade.ecommerce.repository.AuditoriaLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Usa injeção por construtor (via @RequiredArgsConstructor) e acessa o usuário logado
// direto pelo SecurityContextHolder, eliminando a dependência circular com UsuarioService
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaLogRepository auditoriaLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Registra um log de auditoria
     * REQUIRES_NEW garante que o log seja salvo mesmo se a transação principal falhar
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String entidade, Long entidadeId, TipoOperacao operacao,
                         Object dadosAnteriores, Object dadosNovos, String observacao) {
        try {
            // Obtém o email direto do SecurityContext, sem depender de outro Service
            String email = "SISTEMA";
            Long usuarioId = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                email = auth.getName();
            }

            String dadosAnterioresJson = dadosAnteriores != null ?
                    objectMapper.writeValueAsString(dadosAnteriores) : null;

            String dadosNovosJson = dadosNovos != null ?
                    objectMapper.writeValueAsString(dadosNovos) : null;

            AuditoriaLog auditLog = AuditoriaLog.builder()
                    .entidade(entidade)
                    .entidadeId(entidadeId)
                    .operacao(operacao)
                    .usuarioEmail(email)
                    .usuarioId(usuarioId)
                    .dataHora(LocalDateTime.now())
                    .dadosAnteriores(dadosAnterioresJson)
                    .dadosNovos(dadosNovosJson)
                    .observacao(observacao)
                    .build();

            auditoriaLogRepository.save(auditLog);
        } catch (Exception e) {
            // Loga o erro sem interromper a operação principal
            log.error("Erro ao registrar auditoria: {}", e.getMessage(), e);
        }
    }

    /**
     * Registra log simplificado (sem dados anteriores/novos)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String entidade, Long entidadeId, TipoOperacao operacao, String observacao) {
        registrar(entidade, entidadeId, operacao, null, null, observacao);
    }

    /**
     * Lista todos os logs
     */
    public List<AuditoriaLog> listarTodos() {
        return auditoriaLogRepository.findTop100ByOrderByDataHoraDesc();
    }

    /**
     * Busca logs por entidade
     */
    public List<AuditoriaLog> buscarPorEntidade(String entidade) {
        return auditoriaLogRepository.findByEntidadeOrderByDataHoraDesc(entidade);
    }

    /**
     * Busca logs de uma entidade específica
     */
    public List<AuditoriaLog> buscarPorEntidadeId(String entidade, Long entidadeId) {
        return auditoriaLogRepository.findByEntidadeAndEntidadeIdOrderByDataHoraDesc(entidade, entidadeId);
    }

    /**
     * Busca logs por usuário
     */
    public List<AuditoriaLog> buscarPorUsuario(Long usuarioId) {
        return auditoriaLogRepository.findByUsuarioIdOrderByDataHoraDesc(usuarioId);
    }

    /**
     * Busca logs por período
     */
    public List<AuditoriaLog> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return auditoriaLogRepository.findByPeriodo(inicio, fim);
    }
}
