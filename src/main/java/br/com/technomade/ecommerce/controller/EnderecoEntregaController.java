package br.com.technomade.ecommerce.controller;


import br.com.technomade.ecommerce.dto.endereco.EnderecoEntregaRequestDTO;
import br.com.technomade.ecommerce.dto.endereco.EnderecoEntregaResponseDTO;
import br.com.technomade.ecommerce.model.EnderecoEntrega;
import br.com.technomade.ecommerce.model.Usuario;
import br.com.technomade.ecommerce.service.EnderecoEntregaService;
import br.com.technomade.ecommerce.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/enderecos")
public class EnderecoEntregaController {

    @Autowired
    private EnderecoEntregaService enderecoEntregaService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public List<EnderecoEntregaResponseDTO> listar(){
        return enderecoEntregaService.listarEnderecos().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @PostMapping
    public EnderecoEntregaResponseDTO cadastrar(@RequestBody EnderecoEntregaRequestDTO dto){
        Usuario usuario = usuarioService.getUsuarioLogado();
        EnderecoEntrega enderecoEntrega = toEntity(dto);
        enderecoEntrega.setUsuario(usuario);
        return toResponseDTO(enderecoEntregaService.salvar(enderecoEntrega));
    }

    @PutMapping("/{id}")
    public EnderecoEntregaResponseDTO atualizar(@PathVariable Long id, @RequestBody EnderecoEntregaRequestDTO dto){
        EnderecoEntrega enderecoEntregaAtualizado = enderecoEntregaService.atualizar(id,dto);
        return toResponseDTO(enderecoEntregaAtualizado);
    }

    // metodo para converter dto em entidade
    private EnderecoEntrega toEntity(EnderecoEntregaRequestDTO dto){
        return EnderecoEntrega.builder()
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
