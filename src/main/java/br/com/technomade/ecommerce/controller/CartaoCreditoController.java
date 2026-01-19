package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.cartao.CartaoCreditoRequestDTO;
import br.com.technomade.ecommerce.dto.cartao.CartaoCreditoResponseDTO;
import br.com.technomade.ecommerce.dto.cartao.CartaoCreditoUpdateDTO;
import br.com.technomade.ecommerce.model.CartaoCredito;
import br.com.technomade.ecommerce.service.CartaoCreditoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cartoes")
public class CartaoCreditoController {

    @Autowired
    private CartaoCreditoService cartaoCreditoService;

    @GetMapping
    public List<CartaoCreditoResponseDTO> listar(){
        return cartaoCreditoService.listarCartoes().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @PostMapping
    public CartaoCreditoResponseDTO cadastrar(@RequestBody CartaoCreditoRequestDTO dto){
        CartaoCredito cartao = toEntity(dto);
        CartaoCredito cartaoSalvo = cartaoCreditoService.salvar(cartao);
        return toResponseDTO(cartaoSalvo);
    }

    @PutMapping("/{id}")
    public CartaoCreditoResponseDTO atualizar(@PathVariable Long id, @RequestBody CartaoCreditoUpdateDTO dto){
        CartaoCredito cartaoCreditoAtualizado = cartaoCreditoService.atualizar(id, dto.getNomeImpresso(), dto.isPreferencial());
        return toResponseDTO(cartaoCreditoAtualizado);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id){
        cartaoCreditoService.deletar(id);
    }

    // metodo para converter dto em entidade
    private CartaoCredito toEntity(CartaoCreditoRequestDTO dto){
        return CartaoCredito.builder()
                .numero(dto.getNumero())
                .nomeImpresso(dto.getNomeImpresso())
                .bandeira(dto.getBandeira())
                .codigoSeguranca(dto.getCodigoSegurança())
                .preferencial(dto.isPreferencial())
                .build();
    }

    // metodo para converter entidade em dto
    private CartaoCreditoResponseDTO toResponseDTO(CartaoCredito cartaoCredito){
        return CartaoCreditoResponseDTO.builder()
                .id(cartaoCredito.getId())
                .numero(cartaoCredito.getNumero())
                .nomeImpresso(cartaoCredito.getNomeImpresso())
                .bandeira(cartaoCredito.getBandeira())
                .preferencial(cartaoCredito.isPreferencial())
                .build();
    }
}
