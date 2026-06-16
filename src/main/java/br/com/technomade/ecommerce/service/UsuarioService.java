package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.dto.endereco.EnderecoEntregaRequestDTO;
import br.com.technomade.ecommerce.dto.usuario.UsuarioRequestDTO;
import br.com.technomade.ecommerce.dto.usuario.UsuarioUpdateDTO;
import br.com.technomade.ecommerce.model.*;
import br.com.technomade.ecommerce.repository.ClienteRepository;
import br.com.technomade.ecommerce.repository.EnderecoEntregaRepository;
import br.com.technomade.ecommerce.repository.UsuarioRepository;
import br.com.technomade.ecommerce.repository.spec.UsuarioSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final EnderecoEntregaRepository enderecoEntregaRepository;
    private final AuditoriaService auditoriaService;
    private final PasswordEncoder passwordEncoder;

    // validar senha
    private void validarSenhaForte(String senha) {
        if (senha == null || senha.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter no minimo 8 caracteres");
        }
        if (!senha.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("A senha deve conter pelo menos uma letra maiuscula");
        }
        if (!senha.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("A senha deve conter pel menos uma letra minuscula");
        }
        if (!senha.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new IllegalArgumentException("A senha deve conter pelo menos um caractere especial (!@#$%^&*()_+-=[]{}|;':\",./<>?)");
        }
    }

    // salvar novo usuario (e criar Cliente se role=CLIENTE)
    @Transactional
    public Usuario salvar(Usuario usuario, UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(usuario.getEmail())){
            throw new IllegalArgumentException("email já cadastrado");
        }

        // valida a senha
        validarSenhaForte(usuario.getSenha());

        // validar data de nascimento
        if (dto.getDataNascimento() != null && dto.getDataNascimento().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data de nascimento não pode ser futura");
        }

        // criptografar a senha antes de salvar (hash bcrypt)
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        // se for CLIENTE, criar a entidade cliente associada
        if (usuarioSalvo.getRole() == Role.CLIENTE) {
            Cliente cliente = Cliente.builder()
                    .usuario(usuarioSalvo)
                    .nome(usuarioSalvo.getNome())
                    .email(usuarioSalvo.getEmail())
                    .genero(dto.getGenero())
                    .dataNascimento(dto.getDataNascimento())
                    .cpf(dto.getCpf())
                    .telefone(dto.getTelefone())
                    .build();
            clienteRepository.save(cliente);

            // salvar enderecos de entrega enviados no cadastro
            List<EnderecoEntregaRequestDTO> enderecosEntrega = dto.getEnderecosEntrega();
            if (enderecosEntrega != null && !enderecosEntrega.isEmpty()) {
                for (EnderecoEntregaRequestDTO endDto : enderecosEntrega) {
                    EnderecoEntrega enderecoEntrega = EnderecoEntrega.builder()
                            .nomeEndereco(endDto.getNomeEndereco())
                            .tipoResidencia(endDto.getTipoResidencia())
                            .tipoLogradouro(endDto.getTipoLogradouro())
                            .logradouro(endDto.getLogradouro())
                            .numero(endDto.getNumero())
                            .bairro(endDto.getBairro())
                            .cep(endDto.getCep())
                            .cidade(endDto.getCidade())
                            .estado(endDto.getEstado())
                            .pais(endDto.getPais())
                            .observacoes(endDto.getObservacoes())
                            .cliente(cliente)
                            .build();
                    enderecoEntregaRepository.save(enderecoEntrega);
                }
            }
        }

        // auditar a criação
        auditoriaService.registrar("Usuario", usuarioSalvo.getId(), TipoOperacao.CRIACAO,
                null, usuarioSalvo, "Usuário criado: " + usuarioSalvo.getEmail());

        return usuarioSalvo;
    }

    public List<Usuario> listarTodos(){
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(Long id){
        return usuarioRepository.findById(id);
    }

    // atualizar um usuario
    @Transactional
    public Usuario atualizar(Long id, UsuarioUpdateDTO dto){
        Usuario usuario = buscarPorId(id)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado"));

        // guardar estado anterior
        String nomeAnterior = usuario.getNome();

        // atualizar nome no Usuario
        if (dto.getNome() != null) usuario.setNome(dto.getNome());

        // salvar usuario atualizado
        Usuario usuarioAtualizado = usuarioRepository.save(usuario);

        // atualizar campos do Cliente (se existir)
        Optional<Cliente> clienteOpt = clienteRepository.findByUsuario(usuario);
        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();
            String dadosAnteriores = "Nome: " + cliente.getNome() + ", Tel: " + cliente.getTelefone();

            if (dto.getNome() != null) cliente.setNome(dto.getNome());
            if (dto.getEmail() != null) {
                cliente.setEmail(dto.getEmail());
                usuario.setEmail(dto.getEmail());
                usuarioAtualizado = usuarioRepository.save(usuario);
            }
            if (dto.getGenero() != null) cliente.setGenero(dto.getGenero());
            if (dto.getDataNascimento() != null) cliente.setDataNascimento(dto.getDataNascimento());
            if (dto.getCpf() != null) cliente.setCpf(dto.getCpf());
            if (dto.getTelefone() != null) cliente.setTelefone(dto.getTelefone());
            clienteRepository.save(cliente);

            // auditar atualizacão
            auditoriaService.registrar("Usuario", usuarioAtualizado.getId(), TipoOperacao.ATUALIZACAO,
                    dadosAnteriores,
                    "Nome: " + cliente.getNome() + ", Tel: " + cliente.getTelefone(),
                    "Dados do usuário atualizados");
        } else {
            // admin sem perfil de cliente
            auditoriaService.registrar("Usuario", usuarioAtualizado.getId(), TipoOperacao.ATUALIZACAO,
                    "Nome: " + nomeAnterior,
                    "Nome: " + usuarioAtualizado.getNome(),
                    "Dados do usuário atualizados");
        }

        return usuarioAtualizado;
    }

    // deletar usuario por id
    @Transactional
    public void deletarPorId(Long id){
        Usuario usuario = buscarPorId(id)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado"));

        // auditar exclusão
        auditoriaService.registrar("Usuario", id, TipoOperacao.EXCLUSAO,
                usuario, null, "Usuário deletado: " + usuario.getEmail());

        // deletar cliente associado (se existir)
        clienteRepository.findByUsuario(usuario).ifPresent(clienteRepository::delete);

        // deletar
        usuarioRepository.deleteById(id);
    }


    // inativar usuario
    @Transactional
    public Usuario inativar(Long id){
        Usuario usuario = buscarPorId(id)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado"));

        if (!usuario.isAtivo()){
            throw new IllegalStateException("Usuário já está inativo");
        }

        // setar inativo
        usuario.setAtivo(false);

        //salvar inativo
        Usuario usuarioInativado = usuarioRepository.save(usuario);

        // auditar inativaçao
        auditoriaService.registrar("Usuario", usuarioInativado.getId(), TipoOperacao.INATIVACAO,
                "ativo: true", "ativo: false", "Usuário inativado: " + usuarioInativado.getEmail());

        return usuarioInativado;
    }

    // ativar usuario
    @Transactional
    public Usuario ativar(Long id){
        Usuario usuario = buscarPorId(id)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado"));

        if (usuario.isAtivo()){
            throw new IllegalStateException("Usuário já está ativo");
        }

        // setar ativo
        usuario.setAtivo(true);

        // salvar ativo
        Usuario usuarioAtivado = usuarioRepository.save(usuario);

        // auditar ativacão
        auditoriaService.registrar("Usuario", usuarioAtivado.getId(), TipoOperacao.ATIVACAO,
                "ativo: false", "ativo: true", "Usuário ativado: " + usuarioAtivado.getEmail());

        return usuarioAtivado;
    }

    // pegar o usuario logado atual
    public Usuario getUsuarioLogado(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado, email: " + email));
    }

    // pegar o cliente logado (perfil de dominio do usuario logado)
    public Cliente getClienteLogado() {
        Usuario usuario = getUsuarioLogado();
        return clienteRepository.findByUsuario(usuario)
                .orElseThrow(() -> new IllegalStateException("Perfil de cliente não encontrado"));
    }

    public Page<Usuario> listarPaginado(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return usuarioRepository.findAll(pageable);
    }

    public Page<Usuario> listarComFiltros(
            String nome,
            String email,
            Role role,
            Boolean ativo,
            Pageable pageable
    ) {
        return usuarioRepository.findAll(
                UsuarioSpecifications.comFiltros(
                        nome,
                        email,
                        role,
                        ativo
                ),
                pageable
        );
    }

    // buscar o Cliente associado a um Usuario
    public Optional<Cliente> buscarClientePorUsuario(Usuario usuario) {
        return clienteRepository.findByUsuario(usuario);
    }

    public Optional<Cliente> buscarClientePorUsuarioId(Long usuarioId) {
        return clienteRepository.findByUsuarioId(usuarioId);
    }

    // alterar senha do usuario logado
    @Transactional
    public void alterarSenha(String senhaAtual, String novaSenha) {
        Usuario usuario = getUsuarioLogado();

        if (senhaAtual == null || senhaAtual.isBlank()) {
            throw new IllegalArgumentException("Senha atual é obrigatória");
        }
        if (novaSenha == null || novaSenha.isBlank()) {
            throw new IllegalArgumentException("Nova senha é obrigatória");
        }

        // senha atual está certa? (em relação ao banco)
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new IllegalArgumentException("Senha atual inválida");
        }

        // validar nova senha
        validarSenhaForte(novaSenha);

        // criptografar nova senha
        usuario.setSenha(passwordEncoder.encode(novaSenha));

        // salvar nova senha
        usuarioRepository.save(usuario);

        // auditar alteracão de senha
        auditoriaService.registrar("Usuario", usuario.getId(), TipoOperacao.ATUALIZACAO,
                "senha: [PROTEGIDA]", "senha: [ALTERADA]", "Senha alterada pelo usuário: " + usuario.getEmail());
    }
}
