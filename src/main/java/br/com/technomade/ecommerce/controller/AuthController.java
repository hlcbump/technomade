package br.com.technomade.ecommerce.controller;

import br.com.technomade.ecommerce.dto.LoginRequest;
import br.com.technomade.ecommerce.dto.LoginResponse;
import br.com.technomade.ecommerce.model.Usuario;
import br.com.technomade.ecommerce.repository.UsuarioRepository;
import br.com.technomade.ecommerce.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest){

        // busca o usuario
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(loginRequest.getEmail());

        // retorna 401 caso não encontre
        if (usuarioOpt.isEmpty()){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email inválido");
        }

        // caso encontre o usuario
        Usuario usuario = usuarioOpt.get();

        // compara a senha digitada com a senha do banco
        if (!passwordEncoder.matches(loginRequest.getSenha(), usuario.getSenha())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Senha inválida");
        }

        // se email e senha estiver certo, gera o token jwt
        String token = jwtService.gerarToken(usuario.getEmail());

        // retorna o token no corpo
        return ResponseEntity.ok(new LoginResponse(
                token,
                usuario.getEmail(),
                usuario.getRole().name()
        ));
    }
}
