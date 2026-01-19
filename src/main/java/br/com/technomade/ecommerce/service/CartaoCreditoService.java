package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.model.CartaoCredito;
import br.com.technomade.ecommerce.model.Usuario;
import br.com.technomade.ecommerce.repository.CartaoCreditoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartaoCreditoService {

    @Autowired
    private CartaoCreditoRepository cartaoCreditoRepository;

    @Autowired
    private UsuarioService usuarioService;

    public CartaoCredito salvar(CartaoCredito cartaoCredito){
        Usuario usuario = usuarioService.getUsuarioLogado();
        cartaoCredito.setUsuario(usuario);

        if(cartaoCredito.isPreferencial()){
            List<CartaoCredito> todosCartoes = cartaoCreditoRepository.findAllByUsuario(usuario);
            for(CartaoCredito cartao : todosCartoes){
                if (cartao.isPreferencial()){
                    cartao.setPreferencial(false);
                    cartaoCreditoRepository.save(cartao);
                }
            }
        }

        return cartaoCreditoRepository.save(cartaoCredito);

    }

    public List<CartaoCredito> listarCartoes(){
        Usuario usuario = usuarioService.getUsuarioLogado();
        return cartaoCreditoRepository.findAllByUsuario(usuario);
    }

    public CartaoCredito atualizar(Long id, String novoNomeImpresso, boolean preferencial){
        Usuario usuario = usuarioService.getUsuarioLogado();

        CartaoCredito cartaoCredito = cartaoCreditoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cartão não encontrado."));

        if (!cartaoCredito.getUsuario().getId().equals(usuario.getId())){
            throw new RuntimeException("Sem permissão para alterar o cartão");
        }

        cartaoCredito.setNomeImpresso(novoNomeImpresso);

        if(preferencial){
            cartaoCreditoRepository.findAllByUsuario(usuario).forEach(cartao -> {
                if (cartao.isPreferencial()) {
                    cartao.setPreferencial(false);
                    cartaoCreditoRepository.save(cartao);
                }
            });
        }

        return cartaoCreditoRepository.save(cartaoCredito);
    }

    public void deletar(Long id){
        Usuario usuario = usuarioService.getUsuarioLogado();
        CartaoCredito cartaoCredito = cartaoCreditoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cartão não encontrado"));

        if(!cartaoCredito.getUsuario().getId().equals(usuario.getId())){
            throw new RuntimeException("Voce não tem permissão para excluir esse cartão");
        }

        cartaoCreditoRepository.delete(cartaoCredito);
    }
}
