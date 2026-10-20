package clinica.gateway.controller;

import clinica.gateway.config.AppProperties;
import clinica.gateway.dto.AuthDtos.LoginRequest;
import clinica.gateway.dto.AuthDtos.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/** Login público: confere usuário/senha configurados e emite um JWT HS256. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AppProperties.Auth auth;
    private final AppProperties.Jwt jwt;
    private final JwtEncoder encoder;

    public AuthController(AppProperties.Auth auth, AppProperties.Jwt jwt, JwtEncoder encoder) {
        this.auth = auth;
        this.jwt = jwt;
        this.encoder = encoder;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        if (!auth.username().equals(req.usuario()) || !auth.password().equals(req.senha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos.");
        }
        Instant agora = Instant.now();
        // Claims mínimas: quem é (subject) e até quando vale. A senha nunca entra no token.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(req.usuario())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(jwt.expirationSeconds()))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new LoginResponse(token, "Bearer", jwt.expirationSeconds());
    }
}
