package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.dto.notificacao.NotificacaoResponseDTO;
import br.com.technomade.ecommerce.model.Notificacao;
import br.com.technomade.ecommerce.model.TipoNotificacao;
import br.com.technomade.ecommerce.model.Usuario;
import br.com.technomade.ecommerce.repository.NotificacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificacaoService {

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Cria uma nova notificação para um usuário
     */
    @Transactional
    public Notificacao criar(Usuario usuario, TipoNotificacao tipo, String mensagem) {
        return criar(usuario, tipo, mensagem, null, null);
    }

    /**
     * Cria uma nova notificação com referência
     */
    @Transactional
    public Notificacao criar(Usuario usuario, TipoNotificacao tipo, String mensagem,
                            Long referenciaId, String referenciaTipo) {
        Notificacao notificacao = Notificacao.builder()
                .usuario(usuario)
                .tipo(tipo)
                .mensagem(mensagem)
                .lida(false)
                .dataHora(LocalDateTime.now())
                .referenciaId(referenciaId)
                .referenciaTipo(referenciaTipo)
                .build();

        return notificacaoRepository.save(notificacao);
    }

    /**
     * Lista todas as notificações do usuário autenticado
     */
    public List<NotificacaoResponseDTO> listarMinhasNotificacoes() {
        Usuario usuario = usuarioService.getUsuarioLogado();
        List<Notificacao> notificacoes = notificacaoRepository.findByUsuarioOrderByDataHoraDesc(usuario);
        return notificacoes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista notificações não lidas do usuário autenticado
     */
    public List<NotificacaoResponseDTO> listarNaoLidas() {
        Usuario usuario = usuarioService.getUsuarioLogado();
        List<Notificacao> notificacoes = notificacaoRepository.findByUsuarioAndLidaFalseOrderByDataHoraDesc(usuario);
        return notificacoes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Conta notificações não lidas do usuário autenticado
     */
    public Long contarNaoLidas() {
        Usuario usuario = usuarioService.getUsuarioLogado();
        return notificacaoRepository.countByUsuarioAndLidaFalse(usuario);
    }

    /**
     * Marca uma notificação como lida
     */
    @Transactional
    public NotificacaoResponseDTO marcarComoLida(Long notificacaoId) {
        Usuario usuario = usuarioService.getUsuarioLogado();
        Notificacao notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

        // Verifica se a notificação pertence ao usuário logado
        if (!notificacao.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("Você não tem permissão para marcar esta notificação");
        }

        notificacao.setLida(true);
        notificacaoRepository.save(notificacao);

        return toDTO(notificacao);
    }

    /**
     * Marca todas as notificações do usuário como lidas
     */
    @Transactional
    public void marcarTodasComoLidas() {
        Usuario usuario = usuarioService.getUsuarioLogado();
        List<Notificacao> notificacoes = notificacaoRepository.findByUsuarioAndLidaFalseOrderByDataHoraDesc(usuario);

        notificacoes.forEach(n -> n.setLida(true));
        notificacaoRepository.saveAll(notificacoes);
    }

    /**
     * Busca notificação por ID
     */
    public NotificacaoResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioService.getUsuarioLogado();
        Notificacao notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

        // Verifica se a notificação pertence ao usuário logado
        if (!notificacao.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("Você não tem permissão para acessar esta notificação");
        }

        return toDTO(notificacao);
    }

    /**
     * Converte Notificacao para DTO
     */
    private NotificacaoResponseDTO toDTO(Notificacao notificacao) {
        return NotificacaoResponseDTO.builder()
                .id(notificacao.getId())
                .tipo(notificacao.getTipo())
                .mensagem(notificacao.getMensagem())
                .lida(notificacao.getLida())
                .dataHora(notificacao.getDataHora())
                .referenciaId(notificacao.getReferenciaId())
                .referenciaTipo(notificacao.getReferenciaTipo())
                .build();
    }

    /**
     * Notifica usuário sobre troca autorizada
     */
    @Transactional
    public void notificarTrocaAutorizada(Usuario usuario, Long pedidoTrocaId) {
        criar(usuario,
              TipoNotificacao.TROCA_AUTORIZADA,
              "Sua solicitação de troca foi autorizada! Você pode enviar os produtos.",
              pedidoTrocaId,
              "PEDIDO_TROCA");
    }

    /**
     * Notifica usuário sobre carrinho expirando
     */
    @Transactional
    public void notificarCarrinhoExpirando(Usuario usuario, int minutosRestantes) {
        criar(usuario,
              TipoNotificacao.CARRINHO_EXPIRANDO,
              String.format("Seu carrinho expira em %d minutos! Finalize sua compra para não perder os produtos.", minutosRestantes),
              null,
              null);
    }

    /**
     * Notifica usuário sobre alteração de estoque
     */
    @Transactional
    public void notificarEstoqueAlterado(Usuario usuario, Long produtoId, String nomeProduto) {
        criar(usuario,
              TipoNotificacao.ESTOQUE_ALTERADO,
              String.format("O produto '%s' teve seu estoque alterado. Verifique a disponibilidade.", nomeProduto),
              produtoId,
              "PRODUTO");
    }

    /**
     * Notifica usuário sobre compra aprovada
     */
    @Transactional
    public void notificarCompraAprovada(Usuario usuario, Long compraId) {
        criar(usuario,
              TipoNotificacao.COMPRA_APROVADA,
              "Sua compra foi aprovada e está sendo processada!",
              compraId,
              "COMPRA");
    }

    /**
     * Notifica usuário sobre compra reprovada
     */
    @Transactional
    public void notificarCompraReprovada(Usuario usuario, Long compraId) {
        criar(usuario,
              TipoNotificacao.COMPRA_REPROVADA,
              "Sua compra foi reprovada. Verifique os dados de pagamento e tente novamente.",
              compraId,
              "COMPRA");
    }

    /**
     * Notifica usuário sobre compra em trânsito
     */
    @Transactional
    public void notificarCompraEmTransito(Usuario usuario, Long compraId) {
        criar(usuario,
              TipoNotificacao.COMPRA_EM_TRANSITO,
              "Sua compra está a caminho! Acompanhe o status da entrega.",
              compraId,
              "COMPRA");
    }

    /**
     * Notifica usuário sobre compra entregue
     */
    @Transactional
    public void notificarCompraEntregue(Usuario usuario, Long compraId) {
        criar(usuario,
              TipoNotificacao.COMPRA_ENTREGUE,
              "Sua compra foi entregue! Esperamos que goste dos produtos.",
              compraId,
              "COMPRA");
    }

    /**
     * Notifica usuário sobre cupom gerado
     */
    @Transactional
    public void notificarCupomGerado(Usuario usuario, String codigoCupom, Double valor) {
        criar(usuario,
              TipoNotificacao.CUPOM_GERADO,
              String.format("Um cupom de troca no valor de R$ %.2f foi gerado! Código: %s", valor, codigoCupom),
              null,
              "CUPOM");
    }
}
