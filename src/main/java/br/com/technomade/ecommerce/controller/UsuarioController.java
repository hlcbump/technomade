package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.usuario.AlterarSenhaRequestDTO;
import br.com.technomade.ecommerce.dto.usuario.UsuarioRequestDTO;
import br.com.technomade.ecommerce.dto.usuario.UsuarioResponseDTO;
import br.com.technomade.ecommerce.dto.usuario.UsuarioUpdateDTO;
import br.com.technomade.ecommerce.dto.compra.CompraResumoDTO;
import br.com.technomade.ecommerce.dto.endereco.EnderecoEntregaRequestDTO;
import br.com.technomade.ecommerce.model.Genero;
import br.com.technomade.ecommerce.model.Role;
import br.com.technomade.ecommerce.model.Usuario;
import br.com.technomade.ecommerce.model.Compra;
import br.com.technomade.ecommerce.model.EnderecoEntrega;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import br.com.technomade.ecommerce.service.UsuarioService;
import br.com.technomade.ecommerce.service.CompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"http://localhost:8000"})
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CompraService compraService;

    @GetMapping
    public Page<UsuarioResponseDTO> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String telefone,
            @RequestParam(required = false) String endereco,
            @RequestParam(required = false) Genero genero,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascimento
    ) {

        Page<Usuario> usuarios = usuarioService.listarComFiltros(
                nome,
                email,
                cpf,
                telefone,
                endereco,
                genero,
                role,
                ativo,
                dataNascimento,
                PageRequest.of(page, size)
        );

        return usuarios.map(this::toResponseDTO);
    }

    // alteracão apenas de senha
    @PutMapping("/senha")
    public void alterarSenha(@RequestBody AlterarSenhaRequestDTO dto){
        usuarioService.alterarSenha(dto.getSenhaAtual(), dto.getNovaSenha());
    }

    // consulta de transacões do cliente
    @GetMapping("/{id}/transacoes")
    public List<CompraResumoDTO> listarTransacoes(@PathVariable Long id){
        usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario não encontrado"));

        List<Compra> compras = compraService.listarPorCliente(id);
        return compras.stream()
                .map(this::toResumoDTO)
                .collect(Collectors.toList());
    }



    // pathvariable pega valores dinamicos da URL e passa como argumento no controller
    @GetMapping("/{id}")
    public UsuarioResponseDTO buscarPorId(@PathVariable Long id){
        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario não encontrado"));
        return toResponseDTO(usuario);
    }

    @PostMapping
    // requestbody converte o json enviado pelo postman em um objeto
    public UsuarioResponseDTO cadastrar(@RequestBody UsuarioRequestDTO dto){
        Usuario usuario = toEntity(dto);
        Usuario salvo = usuarioService.salvar(usuario);
        return toResponseDTO(salvo);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id){
        usuarioService.deletarPorId(id);
    }

    @PutMapping("/{id}/inativar")
    public UsuarioResponseDTO inativar(@PathVariable Long id){
        Usuario usuario = usuarioService.inativar(id);
        return toResponseDTO(usuario);
    }

    @PutMapping("/{id}/ativar")
    public UsuarioResponseDTO ativar(@PathVariable Long id){
        Usuario usuario = usuarioService.ativar(id);
        return toResponseDTO(usuario);
    }

    @PutMapping("/{id}")
    public UsuarioResponseDTO atualizar(@PathVariable Long id, @RequestBody UsuarioUpdateDTO dto){
        Usuario usuarioAtualizado = usuarioService.atualizar(id,dto);
        return toResponseDTO(usuarioAtualizado);
    }

    // metodo para converter dto em entidade
    private Usuario toEntity(UsuarioRequestDTO dto){
        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .genero(dto.getGenero())
                .dataNascimento(dto.getDataNascimento())
                .email(dto.getEmail())
                .senha(dto.getSenha())
                .role(dto.getRole())
                .cpf(dto.getCpf())
                .telefone(dto.getTelefone())
                .endereco(dto.getEndereco())
                .build();

        if (dto.getEnderecosEntrega() != null) {
            List<EnderecoEntrega> enderecos = dto.getEnderecosEntrega().stream()
                    .map(enderecoDto -> toEnderecoEntrega(enderecoDto, usuario))
                    .collect(Collectors.toList());
            usuario.setEnderecosEntrega(enderecos);
        }

        return usuario;
    }

    private EnderecoEntrega toEnderecoEntrega(EnderecoEntregaRequestDTO dto, Usuario usuario) {
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
                .usuario(usuario)
                .build();
    }

    // metodo para converter entidade em dto
    private UsuarioResponseDTO toResponseDTO(Usuario usuario){
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .genero(usuario.getGenero())
                .dataNascimento(usuario.getDataNascimento())
                .email(usuario.getEmail())
                .role(usuario.getRole())
                .cpf(usuario.getCpf())
                .telefone(usuario.getTelefone())
                .endereco(usuario.getEndereco())
                .ativo(usuario.isAtivo())
                .build();
    }

    private CompraResumoDTO toResumoDTO(Compra compra){
        return new CompraResumoDTO(
                compra.getId(),
                compra.getStatusCompra(),
                compra.getValorTotal(),
                compra.getDataCompra()
        );
    }
}
