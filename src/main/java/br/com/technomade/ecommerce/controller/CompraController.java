package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.compra.CompraRequestDTO;
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

    // finalizar compra - cliente
    @PostMapping
    public ResponseEntity<Compra> finalizarCompra(@RequestBody CompraRequestDTO dto){
        // buscar o usuario
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario não encontrado"));

        // buscar o endereco de entrega
        EnderecoEntrega enderecoEntrega = enderecoEntregaRepository.findById(dto.getEnderecoEntregaId())
                .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado."));

        // converte os pagamentos recebidos via DTO para entidade
        List<Pagamento> pagamentos = dto.getPagamentos().stream()
                .map(p -> {
                    Pagamento pagamento = new Pagamento();
                    pagamento.setFormaPagamento(FormaPagamento.valueOf(p.getFormaPagamento()));
                    pagamento.setValor(p.getValor());

                    // se tiver cartão de crédito, busca no banco
                    if (p.getCartaoCreditoId() != null){
                        CartaoCredito cartaoCredito = cartaoCreditoRepository.findById(p.getCartaoCreditoId())
                                .orElseThrow(() -> new IllegalArgumentException("Cartão de crédito não encontrado"));
                        pagamento.setCartaoCredito(cartaoCredito);
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

