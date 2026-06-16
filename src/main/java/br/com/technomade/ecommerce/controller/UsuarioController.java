package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.usuario.AlterarSenhaRequestDTO;
import br.com.technomade.ecommerce.dto.usuario.UsuarioRequestDTO;
import br.com.technomade.ecommerce.dto.usuario.UsuarioResponseDTO;
import br.com.technomade.ecommerce.dto.usuario.UsuarioUpdateDTO;
import br.com.technomade.ecommerce.dto.compra.CompraResumoDTO;
import br.com.technomade.ecommerce.model.Cliente;
import br.com.technomade.ecommerce.model.Role;
import br.com.technomade.ecommerce.model.Usuario;
import br.com.technomade.ecommerce.model.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import br.com.technomade.ecommerce.service.UsuarioService;
import br.com.technomade.ecommerce.service.CompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean ativo
    ) {

        Page<Usuario> usuarios = usuarioService.listarComFiltros(
                nome,
                email,
                role,
                ativo,
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

        // buscar o cliente associado ao usuario para pegar o clienteId
        Optional<Cliente> clienteOpt = usuarioService.buscarClientePorUsuarioId(id);
        if (clienteOpt.isEmpty()) {
            return List.of();
        }

        List<Compra> compras = compraService.listarPorCliente(clienteOpt.get().getId());
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
        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senha(dto.getSenha())
                .role(dto.getRole() != null ? dto.getRole() : Role.CLIENTE)
                .build();

        Usuario salvo = usuarioService.salvar(usuario, dto);
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

    // metodo para converter entidade em dto (popula dados do Cliente quando role=CLIENTE)
    private UsuarioResponseDTO toResponseDTO(Usuario usuario){
        UsuarioResponseDTO.UsuarioResponseDTOBuilder builder = UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .role(usuario.getRole())
                .ativo(usuario.isAtivo());

        // buscar dados do Cliente se for role CLIENTE
        if (usuario.getRole() == Role.CLIENTE) {
            usuarioService.buscarClientePorUsuario(usuario).ifPresent(cliente -> {
                builder.genero(cliente.getGenero());
                builder.dataNascimento(cliente.getDataNascimento());
                builder.cpf(cliente.getCpf());
                builder.telefone(cliente.getTelefone());
            });
        }

        return builder.build();
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
