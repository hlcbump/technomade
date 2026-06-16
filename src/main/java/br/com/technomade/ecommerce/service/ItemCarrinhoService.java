package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.dto.carrinho.ItemCarrinhoRequestDTO;
import br.com.technomade.ecommerce.dto.carrinho.ItemCarrinhoResponseDTO;
import br.com.technomade.ecommerce.dto.carrinho.ItemCarrinhoUpdateDTO;
import br.com.technomade.ecommerce.model.Cliente;
import br.com.technomade.ecommerce.model.Estoque;
import br.com.technomade.ecommerce.model.ItemCarrinho;
import br.com.technomade.ecommerce.model.Produto;
import br.com.technomade.ecommerce.repository.EstoqueRepository;
import br.com.technomade.ecommerce.repository.ItemCarrinhoRepository;
import br.com.technomade.ecommerce.repository.ProdutoRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemCarrinhoService {

    @Autowired
    private ItemCarrinhoRepository itemCarrinhoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private NotificacaoService notificacaoService;

    private int minutosBloqueio = 30;
    private int minutosAvisoExpiracao = 5; // Aviso 5 minutos antes de expirar (RN0044)

    private LocalDateTime novaReserva(){
        return LocalDateTime.now().plusMinutes(minutosBloqueio);
    }

    @Transactional
    public ItemCarrinhoResponseDTO adicionar(ItemCarrinhoRequestDTO dto){
        Cliente cliente = usuarioService.getClienteLogado();
        Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        int quantidadeSolicitada = dto.getQuantidade();
        validarEstoqueDisponivel(produto, quantidadeSolicitada); //RN0031

        ItemCarrinho item = itemCarrinhoRepository
                .findByClienteAndProduto(cliente, produto)
                .orElse(ItemCarrinho.builder()
                        .cliente(cliente)
                        .produto(produto)
                        .quantidade(0)
                        .build());

        int novaQuantidade = item.getQuantidade() + quantidadeSolicitada;
        validarEstoqueDisponivel(produto, novaQuantidade); //RN0031

        item.setQuantidade(novaQuantidade);
        item.setReservadoAte(novaReserva());

        ItemCarrinho salvo = itemCarrinhoRepository.save(item);
        return toResponse(salvo);

    }

    @Transactional
    public ItemCarrinhoResponseDTO atualizarQuantidade(Long itemId, ItemCarrinhoUpdateDTO dto){
        Cliente cliente = usuarioService.getClienteLogado();

        ItemCarrinho item = itemCarrinhoRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        if(!item.getCliente().getId().equals(cliente.getId())){
            throw new RuntimeException("Voce não tem permissão para atualizar esse carrinho");
        }

        int novaQuantidade = dto.getQuantidade();
        if(novaQuantidade <= 0){
            itemCarrinhoRepository.delete(item);
            throw new RuntimeException("Quantidade inválida.");
        }

        //RN0032
        validarEstoqueDisponivel(item.getProduto(), novaQuantidade);

        item.setQuantidade(novaQuantidade);
        item.setReservadoAte(novaReserva());

        ItemCarrinho salvo = itemCarrinhoRepository.save(item);
        return toResponse(salvo);

    }

    @Transactional
    public void remover(Long id){
        Cliente cliente = usuarioService.getClienteLogado();

        ItemCarrinho itemCarrinho = itemCarrinhoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        if(!itemCarrinho.getCliente().getId().equals(cliente.getId())){
            throw new RuntimeException("Você não tem permissão para excluir esse item");
        }

        itemCarrinhoRepository.delete(itemCarrinho);
    }

    @Transactional
    public List<ItemCarrinhoResponseDTO> listarDoUsuario(){
        Cliente cliente = usuarioService.getClienteLogado();

        // antes de listar, limpa expirados
        List<ItemCarrinho> expirados = itemCarrinhoRepository
                .findAllByClienteAndReservadoAteBefore(cliente, LocalDateTime.now());
        if (!expirados.isEmpty()){
            itemCarrinhoRepository.deleteAll(expirados);
        }

        return itemCarrinhoRepository.findAllByCliente(cliente).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void validarEstoqueDisponivel(Produto produto, int quantidadeNecessaria){
        Estoque estoque = estoqueRepository.findByProdutoId(produto.getId())
                .orElseThrow(() -> new RuntimeException("Estoque não encontrado"));

        if(estoque.getQuantidade() == null || estoque.getQuantidade() < quantidadeNecessaria){
            throw new RuntimeException("Quantidade indisponivel em estoque"); //RN0031 e RN0032
        }

    }

    private ItemCarrinhoResponseDTO toResponse(ItemCarrinho itemCarrinho){
        return ItemCarrinhoResponseDTO.builder()
                .id(itemCarrinho.getId())
                .produtoId(itemCarrinho.getProduto().getId())
                .nomeProduto(itemCarrinho.getProduto().getNome())
                .quantidade(itemCarrinho.getQuantidade())
                .reservadoAte(itemCarrinho.getReservadoAte())
                .build();
    }

    /**
     * Calcula minutos restantes até expiração do item mais próximo de expirar
     * Retorna -1 se carrinho vazio ou todos expirados
     */
    public long calcularMinutosRestantes() {
        Cliente cliente = usuarioService.getClienteLogado();
        List<ItemCarrinho> itens = itemCarrinhoRepository.findAllByCliente(cliente);

        if (itens.isEmpty()) {
            return -1;
        }

        LocalDateTime agora = LocalDateTime.now();

        // Encontra o item que expira primeiro
        LocalDateTime menorExpiracao = itens.stream()
                .map(ItemCarrinho::getReservadoAte)
                .filter(expiracao -> expiracao.isAfter(agora))
                .min(LocalDateTime::compareTo)
                .orElse(null);

        if (menorExpiracao == null) {
            return 0; // Todos expirados
        }

        return java.time.Duration.between(agora, menorExpiracao).toMinutes();
    }

    /**
     * Verifica se deve notificar usuário (RN0044 - 5 minutos antes)
     */
    public boolean deveNotificarExpiracao() {
        long minutosRestantes = calcularMinutosRestantes();
        return minutosRestantes > 0 && minutosRestantes <= minutosAvisoExpiracao;
    }

    /**
     * Notifica usuário sobre carrinho expirando (chamado pelo frontend)
     */
    @Transactional
    public void notificarSeProximoExpiracao() {
        if (deveNotificarExpiracao()) {
            Cliente cliente = usuarioService.getClienteLogado();
            long minutosRestantes = calcularMinutosRestantes();
            notificacaoService.notificarCarrinhoExpirando(cliente.getUsuario(), (int) minutosRestantes);
        }
    }
}
