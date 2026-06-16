package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.dto.endereco.EnderecoEntregaRequestDTO;
import br.com.technomade.ecommerce.model.Cliente;
import br.com.technomade.ecommerce.model.EnderecoEntrega;
import br.com.technomade.ecommerce.repository.EnderecoEntregaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class EnderecoEntregaService {

    private static final Set<String> TIPOS_ENDERECO = Set.of("ENTREGA", "COBRANCA", "AMBOS");

    @Autowired
    private EnderecoEntregaRepository enderecoEntregaRepository;

    @Autowired
    private UsuarioService usuarioService;

    public EnderecoEntrega salvar(EnderecoEntrega enderecoEntrega){
        validarEndereco(enderecoEntrega);
        Cliente cliente = usuarioService.getClienteLogado();
        enderecoEntrega.setCliente(cliente);
        return enderecoEntregaRepository.save(enderecoEntrega);
    }

    public List<EnderecoEntrega> listarEnderecos(){
        Cliente cliente = usuarioService.getClienteLogado();
        return enderecoEntregaRepository.findAllByCliente(cliente);
    }

    public EnderecoEntrega atualizar(Long id, EnderecoEntregaRequestDTO dto){
        Cliente cliente = usuarioService.getClienteLogado();
        EnderecoEntrega enderecoEntrega = enderecoEntregaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Endereço não encontrado"));

        if (!enderecoEntrega.getCliente().getId().equals(cliente.getId())){
            throw new RuntimeException("Sem permissão para editar o endereço");
        }

        if (dto.getTipoEndereco() != null) {
            enderecoEntrega.setTipoEndereco(dto.getTipoEndereco());
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

        validarEndereco(enderecoEntrega);

        return enderecoEntregaRepository.save(enderecoEntrega);
    }

    public void deletar(Long id){
        Cliente cliente = usuarioService.getClienteLogado();
        EnderecoEntrega enderecoEntrega = enderecoEntregaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Endereço não encontrado"));

        if (!enderecoEntrega.getCliente().getId().equals(cliente.getId())) {
            throw new RuntimeException("Voce não tem permissão para excluir esse endereco");
        }

        enderecoEntregaRepository.delete(enderecoEntrega);
    }

    // RN0021/RN0022/RN0023 - Validar composicao do registro de enderecos
    private void validarEndereco(EnderecoEntrega endereco) {
        // Validar tipo de endereco
        if (endereco.getTipoEndereco() == null || endereco.getTipoEndereco().isBlank()) {
            endereco.setTipoEndereco("ENTREGA");
        }
        String tipo = endereco.getTipoEndereco().trim().toUpperCase();
        if (!TIPOS_ENDERECO.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de endereço inválido. Valores aceitos: ENTREGA, COBRANCA, AMBOS");
        }
        endereco.setTipoEndereco(tipo);

        if (endereco.getCep() != null) {
            String cepLimpo = endereco.getCep().replaceAll("[^\\d]", "");
            if (cepLimpo.length() != 8) {
                throw new IllegalArgumentException("CEP inválido. Deve conter 8 dígitos.");
            }
            endereco.setCep(cepLimpo.substring(0, 5) + "-" + cepLimpo.substring(5));
        }

        if (endereco.getTipoResidencia() == null || endereco.getTipoResidencia().isBlank()) {
            throw new IllegalArgumentException("O tipo de residência é obrigatório.");
        }

        if (endereco.getTipoLogradouro() == null || endereco.getTipoLogradouro().isBlank()) {
            throw new IllegalArgumentException("O tipo de logradouro é obrigatório.");
        }

        if (endereco.getPais() == null || endereco.getPais().isBlank()) {
            throw new IllegalArgumentException("O país é obrigatório.");
        }
    }
}
