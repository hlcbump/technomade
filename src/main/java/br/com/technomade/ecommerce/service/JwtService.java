package br.com.technomade.ecommerce.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    // chave para assinar e validar o token jwt
    // gerar string hexadecimal com 32 bytes = openssl rand -hex 32
    private final String secret = "7d3272e5edc6896636758f6f2cf269c06683da6e6b7958a8021b052db9b65fbe";
    // tempo de expiração do token em ms (10hrs)
    private final long expirationMs = 1000 * 60 * 60 * 10;

    // gera a chave de assinatura usando a chave anterior
    private Key getSignKey(){
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // gera o token com o email e mais alguns parametros
    public String gerarToken(String email){
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // extrai o email de um token usando o token como chave
    public String extrairEmail(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // verificar se o token é valido através de um parse
    public boolean tokenValido(String token){
        try {
            Jwts
                    .parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e){
            return false;
        }
    }
}
