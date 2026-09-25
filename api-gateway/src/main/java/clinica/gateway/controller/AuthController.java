package clinica.gateway.controller;

import clinica.gateway.dto.LoginRequest;
import clinica.gateway.dto.LoginResponse;
import clinica.gateway.dto.UsuarioResponse;
import clinica.gateway.exception.UnauthorizedException;
import clinica.gateway.grpc.CadastroGrpcClient;
import clinica.gateway.security.JwtService;
import clinica.grpc.AutenticarResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final CadastroGrpcClient cadastro;
    private final JwtService jwtService;

    public AuthController(CadastroGrpcClient cadastro, JwtService jwtService) {
        this.cadastro = cadastro;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AutenticarResponse auth = cadastro.autenticar(request.email(), request.senha());
        if (!auth.getAutenticado()) {
            throw new UnauthorizedException(auth.getMensagem());
        }
        UsuarioResponse usuario = new UsuarioResponse(auth.getUsuarioId(), auth.getNome(), auth.getEmail());
        String token = jwtService.gerar(usuario);
        return ResponseEntity.ok(new LoginResponse(token, "Bearer", jwtService.expiracaoSegundos(), usuario));
    }
}
