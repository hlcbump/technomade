package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.compra.CompraRequestDTO;
import br.com.technomade.ecommerce.dto.endereco.EnderecoEntregaRequestDTO;
import br.com.technomade.ecommerce.model.*;
import br.com.technomade.ecommerce.repository.CartaoCreditoRepository;
import br.com.technomade.ecommerce.repository.EnderecoEntregaRepository;
import br.com.technomade.ecommerce.service.CompraService;
import br.com.technomade.ecommerce.service.UsuarioService;
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
    private UsuarioService usuarioService;

    @Autowired
    private EnderecoEntregaRepository enderecoEntregaRepository;

    @Autowired
    private CartaoCreditoRepository cartaoCreditoRepository;

    // listar compras do cliente logado ou todas (admin sem filtro)
    @GetMapping
    public ResponseEntity<List<Compra>> listarCompras(@RequestParam(required = false) Long usuarioId) {
        Usuario usuario = usuarioService.getUsuarioLogado();

        // admin sem filtro: retorna todas as compras
        if (usuario.getRole() == Role.ADMIN && usuarioId == null) {
            return ResponseEntity.ok(compraService.listarTodas());
        }

        // admin com filtro por usuarioId, ou cliente comum
        Cliente cliente;
        if (usuarioId != null && usuario.getRole() == Role.ADMIN) {
            cliente = usuarioService.buscarClientePorUsuarioId(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para o usuário informado"));
        } else {
            cliente = usuarioService.getClienteLogado();
        }
        List<Compra> compras = compraService.listarPorCliente(cliente.getId());
        return ResponseEntity.ok(compras);
    }

    // endpoint para calcular frete
    @GetMapping("/frete")
    public ResponseEntity<Double> calcularFrete(@RequestParam Long enderecoEntregaId){
        Cliente cliente = usuarioService.getClienteLogado();

        EnderecoEntrega enderecoEntrega = enderecoEntregaRepository.findById(enderecoEntregaId)
                .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado."));

        double frete = compraService.calcularFrete(cliente, enderecoEntrega);
        return ResponseEntity.ok(frete);
    }

    // finalizar compra - cliente
    @PostMapping
    public ResponseEntity<Compra> finalizarCompra(@RequestBody CompraRequestDTO dto){
        // buscar o cliente logado via JWT
        Cliente cliente = usuarioService.getClienteLogado();

        // buscar ou criar o endereco de entrega
        EnderecoEntrega enderecoEntrega;
        if (dto.getEnderecoEntregaId() != null) {
            enderecoEntrega = enderecoEntregaRepository.findById(dto.getEnderecoEntregaId())
                    .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado."));
            if (enderecoEntrega.getCliente() == null ||
                    !enderecoEntrega.getCliente().getId().equals(cliente.getId())) {
                throw new IllegalArgumentException("Endereço não pertence ao cliente");
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
                enderecoEntrega.setCliente(cliente);
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
                        if (cartaoCredito.getCliente() == null ||
                                !cartaoCredito.getCliente().getId().equals(cliente.getId())) {
                            throw new IllegalArgumentException("Cartão não pertence ao cliente");
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
                            novoCartao.setCliente(cliente);
                            if (novoCartao.isPreferencial()) {
                                cartaoCreditoRepository.findAllByCliente(cliente).forEach(cartao -> {
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
        Compra compra = compraService.finalizarCompra(cliente, enderecoEntrega, pagamentos);

        return ResponseEntity.ok(compra);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Compra> atualizarStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status é obrigatório");
        }
        StatusCompra novoStatus = StatusCompra.valueOf(status.toUpperCase());
        Compra compraAtualizada = compraService.atualizarStatus(id, novoStatus);

        return ResponseEntity.ok(compraAtualizada);
    }

}
