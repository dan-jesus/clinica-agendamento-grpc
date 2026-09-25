package clinica.gateway.security;

import clinica.gateway.dto.UsuarioResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET deve ter pelo menos 32 bytes.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String gerar(UsuarioResponse usuario) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plus(expirationMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(usuario.email())
                .claim("uid", usuario.id())
                .claim("nome", usuario.nome())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .signWith(key)
                .compact();
    }

    public Claims validarEObterClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long expiracaoSegundos() {
        return expirationMinutes * 60;
    }
}
