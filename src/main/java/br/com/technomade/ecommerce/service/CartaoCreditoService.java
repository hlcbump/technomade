package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.model.CartaoCredito;
import br.com.technomade.ecommerce.model.Cliente;
import br.com.technomade.ecommerce.repository.CartaoCreditoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class CartaoCreditoService {

    // RN0025 - Bandeiras permitidas registradas no sistema
    private static final Set<String> BANDEIRAS_PERMITIDAS = Set.of(
            "VISA", "MASTERCARD", "ELO", "AMEX", "HIPERCARD", "DINERS"
    );

    @Autowired
    private CartaoCreditoRepository cartaoCreditoRepository;

    @Autowired
    private UsuarioService usuarioService;

    public CartaoCredito salvar(CartaoCredito cartaoCredito){
        validarCartao(cartaoCredito);
        Cliente cliente = usuarioService.getClienteLogado();
        cartaoCredito.setCliente(cliente);

        if(cartaoCredito.isPreferencial()){
            List<CartaoCredito> todosCartoes = cartaoCreditoRepository.findAllByCliente(cliente);
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
        Cliente cliente = usuarioService.getClienteLogado();
        return cartaoCreditoRepository.findAllByCliente(cliente);
    }

    public CartaoCredito atualizar(Long id, String novoNomeImpresso, boolean preferencial){
        Cliente cliente = usuarioService.getClienteLogado();

        CartaoCredito cartaoCredito = cartaoCreditoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cartão não encontrado."));

        if (!cartaoCredito.getCliente().getId().equals(cliente.getId())){
            throw new RuntimeException("Sem permissão para alterar o cartão");
        }

        cartaoCredito.setNomeImpresso(novoNomeImpresso);

        if(preferencial){
            cartaoCreditoRepository.findAllByCliente(cliente).forEach(cartao -> {
                if (cartao.isPreferencial()) {
                    cartao.setPreferencial(false);
                    cartaoCreditoRepository.save(cartao);
                }
            });
        }
        cartaoCredito.setPreferencial(preferencial);

        return cartaoCreditoRepository.save(cartaoCredito);
    }

    private void validarCartao(CartaoCredito cartao) {
        // RN0025 - Validar bandeira permitida
        if (cartao.getBandeira() == null || cartao.getBandeira().isBlank()) {
            throw new IllegalArgumentException("A bandeira do cartão é obrigatória.");
        }
        String bandeira = cartao.getBandeira().trim().toUpperCase();
        if (!BANDEIRAS_PERMITIDAS.contains(bandeira)) {
            throw new IllegalArgumentException(
                    "Bandeira '" + cartao.getBandeira() + "' não é permitida. Bandeiras aceitas: " + BANDEIRAS_PERMITIDAS);
        }
        cartao.setBandeira(bandeira);

        // Validar número do cartão (13 a 19 dígitos, algoritmo de Luhn)
        if (cartao.getNumero() == null || cartao.getNumero().isBlank()) {
            throw new IllegalArgumentException("O número do cartão é obrigatório.");
        }
        String numero = cartao.getNumero().replaceAll("\\s+", "").replaceAll("-", "");
        if (!numero.matches("\\d{13,19}")) {
            throw new IllegalArgumentException("Número do cartão inválido. Deve conter entre 13 e 19 dígitos.");
        }
        if (!validarLuhn(numero)) {
            throw new IllegalArgumentException("Número do cartão inválido.");
        }
        cartao.setNumero(numero);

        // Verificar duplicidade de número
        if (cartaoCreditoRepository.existsByNumero(numero)) {
            throw new IllegalArgumentException("Já existe um cartão cadastrado com este número.");
        }

        // Validar código de segurança (3 ou 4 dígitos)
        if (cartao.getCodigoSeguranca() == null || cartao.getCodigoSeguranca().isBlank()) {
            throw new IllegalArgumentException("O código de segurança é obrigatório.");
        }
        if (!cartao.getCodigoSeguranca().matches("\\d{3,4}")) {
            throw new IllegalArgumentException("Código de segurança inválido. Deve conter 3 ou 4 dígitos.");
        }

        // Validar nome impresso (somente letras e espaços)
        if (cartao.getNomeImpresso() == null || cartao.getNomeImpresso().isBlank()) {
            throw new IllegalArgumentException("O nome impresso no cartão é obrigatório.");
        }
        if (!cartao.getNomeImpresso().matches("[a-zA-ZÀ-ÿ\\s]+")) {
            throw new IllegalArgumentException("Nome impresso no cartão deve conter apenas letras e espaços.");
        }
    }

    private boolean validarLuhn(String numero) {
        int soma = 0;
        boolean alternar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int digito = Character.getNumericValue(numero.charAt(i));
            if (alternar) {
                digito *= 2;
                if (digito > 9) {
                    digito -= 9;
                }
            }
            soma += digito;
            alternar = !alternar;
        }
        return (soma % 10 == 0);
    }

    public void deletar(Long id){
        Cliente cliente = usuarioService.getClienteLogado();
        CartaoCredito cartaoCredito = cartaoCreditoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cartão não encontrado"));

        if(!cartaoCredito.getCliente().getId().equals(cliente.getId())){
            throw new RuntimeException("Voce não tem permissão para excluir esse cartão");
        }

        cartaoCreditoRepository.delete(cartaoCredito);
    }
}
