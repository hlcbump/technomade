package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.usuario.UsuarioRequestDTO;
import br.com.technomade.ecommerce.dto.usuario.UsuarioResponseDTO;
import br.com.technomade.ecommerce.dto.usuario.UsuarioUpdateDTO;
import br.com.technomade.ecommerce.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import br.com.technomade.ecommerce.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public Page<UsuarioResponseDTO> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Usuario> usuarios = usuarioService.listarPaginado(page, size);

        return usuarios.map(this::toResponseDTO);
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

    @PutMapping("/{id}")
    public UsuarioResponseDTO atualizar(@PathVariable Long id, @RequestBody UsuarioUpdateDTO dto){
        Usuario usuarioAtualizado = usuarioService.atualizar(id,dto);
        return toResponseDTO(usuarioAtualizado);
    }

    // metodo para converter dto em entidade
    private Usuario toEntity(UsuarioRequestDTO dto){
        return Usuario.builder()
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
}
