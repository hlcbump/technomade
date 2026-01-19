package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.dto.carrinho.ItemCarrinhoRequestDTO;
import br.com.technomade.ecommerce.dto.carrinho.ItemCarrinhoResponseDTO;
import br.com.technomade.ecommerce.dto.carrinho.ItemCarrinhoUpdateDTO;
import br.com.technomade.ecommerce.model.Estoque;
import br.com.technomade.ecommerce.model.ItemCarrinho;
import br.com.technomade.ecommerce.model.Produto;
import br.com.technomade.ecommerce.model.Usuario;
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

    private int minutosBloqueio = 30;

    private LocalDateTime novaReserva(){
        return LocalDateTime.now().plusMinutes(minutosBloqueio);
    }

    // @Transactional
    // essa anotação abre uma transação no método, caso ocorra algum erro
    // todas as alterações dentro do método serão desfeitos
    @Transactional
    public ItemCarrinhoResponseDTO adicionar(ItemCarrinhoRequestDTO dto){
        Usuario usuario = usuarioService.getUsuarioLogado();
        Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        int quantidadeSolicitada = dto.getQuantidade();
        validarEstoqueDisponivel(produto, quantidadeSolicitada); //RN0031

        ItemCarrinho item = itemCarrinhoRepository
                .findByUsuarioAndProduto(usuario,produto)
                .orElse(ItemCarrinho.builder()
                        .usuario(usuario)
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
        Usuario usuario = usuarioService.getUsuarioLogado();

        ItemCarrinho item = itemCarrinhoRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        if(!item.getUsuario().getId().equals(usuario.getId())){
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
        Usuario usuario = usuarioService.getUsuarioLogado();

        ItemCarrinho itemCarrinho = itemCarrinhoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        if(!itemCarrinho.getUsuario().getId().equals(usuario.getId())){
            throw new RuntimeException("Você não tem permissão para excluir esse item");
        }

        itemCarrinhoRepository.delete(itemCarrinho);
    }

    @Transactional
    public List<ItemCarrinhoResponseDTO> listarDoUsuario(){
        Usuario usuario = usuarioService.getUsuarioLogado();

        // antes de listar, limpa expirados
        List<ItemCarrinho> expirados = itemCarrinhoRepository
                .findAllByUsuarioAndReservadoAteBefore(usuario, LocalDateTime.now());
        if (!expirados.isEmpty()){
            itemCarrinhoRepository.deleteAll(expirados);
        }

        return itemCarrinhoRepository.findAllByUsuario(usuario).stream()
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
}
