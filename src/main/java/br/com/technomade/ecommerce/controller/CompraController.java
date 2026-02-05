package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.compra.CompraRequestDTO;
import br.com.technomade.ecommerce.dto.endereco.EnderecoEntregaRequestDTO;
import br.com.technomade.ecommerce.model.*;
import br.com.technomade.ecommerce.repository.CartaoCreditoRepository;
import br.com.technomade.ecommerce.repository.EnderecoEntregaRepository;
import br.com.technomade.ecommerce.repository.UsuarioRepository;
import br.com.technomade.ecommerce.service.CompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    @Autowired
    private CompraService compraService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EnderecoEntregaRepository enderecoEntregaRepository;

    @Autowired
    private CartaoCreditoRepository cartaoCreditoRepository;

    // endpoint para calcular frete
    @GetMapping("/frete")
    public ResponseEntity<Double> calcularFrete(@RequestParam Long usuarioId, @RequestParam Long enderecoEntregaId){
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario não encontrado"));

        EnderecoEntrega enderecoEntrega = enderecoEntregaRepository.findById(enderecoEntregaId)
                .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado."));

        double frete = compraService.calcularFrete(usuario, enderecoEntrega);
        return ResponseEntity.ok(frete);
    }

    // finalizar compra - cliente
    @PostMapping
    public ResponseEntity<Compra> finalizarCompra(@RequestBody CompraRequestDTO dto){
        // buscar o usuario
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario não encontrado"));

        // buscar ou criar o endereco de entrega
        EnderecoEntrega enderecoEntrega;
        if (dto.getEnderecoEntregaId() != null) {
            enderecoEntrega = enderecoEntregaRepository.findById(dto.getEnderecoEntregaId())
                    .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado."));
            if (enderecoEntrega.getUsuario() == null ||
                    !enderecoEntrega.getUsuario().getId().equals(usuario.getId())) {
                throw new IllegalArgumentException("Endereço não pertence ao usuário");
            }
        } else if (dto.getNovoEnderecoEntrega() != null) {
            EnderecoEntregaRequestDTO enderecoDto = dto.getNovoEnderecoEntrega();
            enderecoEntrega = EnderecoEntrega.builder()
                    .nomeEndereco(enderecoDto.getNomeEndereco())
                    .tipoResidencia(enderecoDto.getTipoResidencia())
                    .tipoLogradouro(enderecoDto.getTipoLogradouro())
                    .logradouro(enderecoDto.getLogradouro())
                    .numero(enderecoDto.getNumero())
                    .bairro(enderecoDto.getBairro())
                    .cep(enderecoDto.getCep())
                    .cidade(enderecoDto.getCidade())
                    .estado(enderecoDto.getEstado())
                    .pais(enderecoDto.getPais())
                    .observacoes(enderecoDto.getObservacoes())
                    .build();

            if (Boolean.TRUE.equals(dto.getSalvarEnderecoNoPerfil())) {
                enderecoEntrega.setUsuario(usuario);
            }

            enderecoEntrega = enderecoEntregaRepository.save(enderecoEntrega);
        } else {
            throw new IllegalArgumentException("Endereço de entrega é obrigatório");
        }

        // converte os pagamentos recebidos via DTO para entidade
        List<Pagamento> pagamentos = dto.getPagamentos().stream()
                .map(p -> {
                    Pagamento pagamento = new Pagamento();
                    pagamento.setFormaPagamento(FormaPagamento.valueOf(p.getFormaPagamento()));
                    pagamento.setValor(p.getValor());

                    // se tiver cartão de crédito, busca no banco
                    if (p.getCartaoCreditoId() != null && p.getNovoCartao() != null) {
                        throw new IllegalArgumentException("Informe apenas um cartão por pagamento (id ou novo)");
                    }

                    if (p.getCartaoCreditoId() != null){
                        CartaoCredito cartaoCredito = cartaoCreditoRepository.findById(p.getCartaoCreditoId())
                                .orElseThrow(() -> new IllegalArgumentException("Cartão de crédito não encontrado"));
                        if (cartaoCredito.getUsuario() == null ||
                                !cartaoCredito.getUsuario().getId().equals(usuario.getId())) {
                            throw new IllegalArgumentException("Cartão não pertence ao usuário");
                        }
                        pagamento.setCartaoCredito(cartaoCredito);
                    } else if (p.getNovoCartao() != null) {
                        CartaoCredito novoCartao = CartaoCredito.builder()
                                .numero(p.getNovoCartao().getNumero())
                                .nomeImpresso(p.getNovoCartao().getNomeImpresso())
                                .bandeira(p.getNovoCartao().getBandeira())
                                .codigoSeguranca(p.getNovoCartao().getCodigoSegurança())
                                .preferencial(p.getNovoCartao().isPreferencial())
                                .build();

                        if (Boolean.TRUE.equals(p.getSalvarCartaoNoPerfil())) {
                            novoCartao.setUsuario(usuario);
                            if (novoCartao.isPreferencial()) {
                                cartaoCreditoRepository.findAllByUsuario(usuario).forEach(cartao -> {
                                    if (cartao.isPreferencial()) {
                                        cartao.setPreferencial(false);
                                        cartaoCreditoRepository.save(cartao);
                                    }
                                });
                            }
                        }

                        CartaoCredito cartaoSalvo = cartaoCreditoRepository.save(novoCartao);
                        pagamento.setCartaoCredito(cartaoSalvo);
                    }

                    // se tiver cupom
                    if (p.getCupomCodigo() != null){
                        Cupom cupom = new Cupom();
                        cupom.setCodigo(p.getCupomCodigo());
                        pagamento.setCupom(cupom);
                    }
                    return pagamento;
                })
                .collect(Collectors.toList());

        // chamar o service para finalizar a compra
        Compra compra = compraService.finalizarCompra(usuario, enderecoEntrega, pagamentos);

        return ResponseEntity.ok(compra);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Compra> atualizarStatus(@PathVariable Long id, @RequestParam String status) {
        StatusCompra novoStatus = StatusCompra.valueOf(status.toUpperCase());
        Compra compraAtualizada = compraService.atualizarStatus(id, novoStatus);

        return ResponseEntity.ok(compraAtualizada);
    }

}
