package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.dto.endereco.EnderecoEntregaRequestDTO;
import br.com.technomade.ecommerce.model.EnderecoEntrega;
import br.com.technomade.ecommerce.model.Usuario;
import br.com.technomade.ecommerce.repository.EnderecoEntregaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnderecoEntregaService {

    @Autowired
    private  EnderecoEntregaRepository enderecoEntregaRepository;

    @Autowired
    private  UsuarioService usuarioService;

    public EnderecoEntrega salvar(EnderecoEntrega enderecoEntrega){
        Usuario usuario = usuarioService.getUsuarioLogado();
        enderecoEntrega.setUsuario(usuario);
        return enderecoEntregaRepository.save(enderecoEntrega);
    }

    public List<EnderecoEntrega> listarEnderecos(){
        Usuario usuario = usuarioService.getUsuarioLogado();
        return enderecoEntregaRepository.findAllByUsuario(usuario);
    }

    public EnderecoEntrega atualizar(Long id, EnderecoEntregaRequestDTO dto){
        Usuario usuario = usuarioService.getUsuarioLogado();
        EnderecoEntrega enderecoEntrega = enderecoEntregaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario não encontrado"));

        if (!enderecoEntrega.getUsuario().getId().equals(usuario.getId())){
            throw  new RuntimeException("Sem permissão para editar o endereço");
        }

        enderecoEntrega.setNomeEndereco(dto.getNomeEndereco());
        enderecoEntrega.setTipoResidencia(dto.getTipoResidencia());
        enderecoEntrega.setTipoLogradouro(dto.getTipoLogradouro());
        enderecoEntrega.setLogradouro(dto.getLogradouro());
        enderecoEntrega.setNumero(dto.getNumero());
        enderecoEntrega.setBairro(dto.getBairro());
        enderecoEntrega.setCep(dto.getCep());
        enderecoEntrega.setCidade(dto.getCidade());
        enderecoEntrega.setEstado(dto.getEstado());
        enderecoEntrega.setPais(dto.getPais());
        enderecoEntrega.setObservacoes(dto.getObservacoes());

        return enderecoEntregaRepository.save(enderecoEntrega);
    }
    public void deletar(Long id){
        Usuario usuario = usuarioService.getUsuarioLogado();
        EnderecoEntrega enderecoEntrega = enderecoEntregaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Endereço não encontrado"));

        if (!enderecoEntrega.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("Voce não tem permissão para excluir esse endereco");
        }

        enderecoEntregaRepository.delete(enderecoEntrega);
    }
}
