package clinica.gateway.dto;

public record LoginResponse(String token, String tipo, long expiraEmSegundos, UsuarioResponse usuario) {
}
