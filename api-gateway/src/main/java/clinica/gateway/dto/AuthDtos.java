package clinica.gateway.dto;

import jakarta.validation.constraints.NotBlank;

/** DTOs de autenticação e de erro. */
public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(@NotBlank(message = "usuario é obrigatório") String usuario,
                               @NotBlank(message = "senha é obrigatória") String senha) {}

    public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
}
