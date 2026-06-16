package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.endereco.EnderecoEntregaRequestDTO;
import br.com.technomade.ecommerce.dto.endereco.EnderecoEntregaResponseDTO;
import br.com.technomade.ecommerce.model.EnderecoEntrega;
import br.com.technomade.ecommerce.service.EnderecoEntregaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/enderecos")
public class EnderecoEntregaController {

    @Autowired
    private EnderecoEntregaService enderecoEntregaService;

    @GetMapping
    public List<EnderecoEntregaResponseDTO> listar(){
        return enderecoEntregaService.listarEnderecos().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @PostMapping
    public EnderecoEntregaResponseDTO cadastrar(@RequestBody EnderecoEntregaRequestDTO dto){
        EnderecoEntrega enderecoEntrega = toEntity(dto);
        return toResponseDTO(enderecoEntregaService.salvar(enderecoEntrega));
    }

    @PutMapping("/{id}")
    public EnderecoEntregaResponseDTO atualizar(@PathVariable Long id, @RequestBody EnderecoEntregaRequestDTO dto){
        EnderecoEntrega enderecoEntregaAtualizado = enderecoEntregaService.atualizar(id,dto);
        return toResponseDTO(enderecoEntregaAtualizado);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id){
        enderecoEntregaService.deletar(id);
    }

    // metodo para converter dto em entidade
    private EnderecoEntrega toEntity(EnderecoEntregaRequestDTO dto){
        return EnderecoEntrega.builder()
                .tipoEndereco(dto.getTipoEndereco() != null ? dto.getTipoEndereco() : "ENTREGA")
                .nomeEndereco(dto.getNomeEndereco())
                .tipoResidencia(dto.getTipoResidencia())
                .tipoLogradouro(dto.getTipoLogradouro())
                .logradouro(dto.getLogradouro())
                .numero(dto.getNumero())
                .bairro(dto.getBairro())
                .cep(dto.getCep())
                .cidade(dto.getCidade())
                .estado(dto.getEstado())
                .pais(dto.getPais())
                .observacoes(dto.getObservacoes())
                .build();
    }

    // metodo para converter entidade em dto
    private EnderecoEntregaResponseDTO toResponseDTO(EnderecoEntrega enderecoEntrega){
        return EnderecoEntregaResponseDTO.builder()
                .id(enderecoEntrega.getId())
                .tipoEndereco(enderecoEntrega.getTipoEndereco())
                .nomeEndereco(enderecoEntrega.getNomeEndereco())
                .tipoResidencia(enderecoEntrega.getTipoResidencia())
                .tipoLogradouro(enderecoEntrega.getTipoLogradouro())
                .logradouro(enderecoEntrega.getLogradouro())
                .numero(enderecoEntrega.getNumero())
                .bairro(enderecoEntrega.getBairro())
                .cep(enderecoEntrega.getCep())
                .cidade(enderecoEntrega.getCidade())
                .estado(enderecoEntrega.getEstado())
                .pais(enderecoEntrega.getPais())
                .observacoes(enderecoEntrega.getObservacoes())
                .build();
    }
}
