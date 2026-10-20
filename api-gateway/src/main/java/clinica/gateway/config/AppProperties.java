package clinica.gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Propriedades da aplicação (application.properties / variáveis de ambiente). */
public final class AppProperties {

    private AppProperties() {}

    /** Credenciais do único usuário (APP_USERNAME / APP_PASSWORD). */
    @Validated
    @ConfigurationProperties("app.auth")
    public record Auth(@NotBlank String username, @NotBlank String password) {}

    /** Segredo HS256 (mínimo 32 bytes) e validade do token. */
    @Validated
    @ConfigurationProperties("app.jwt")
    public record Jwt(@Size(min = 32) String secret, @Positive long expirationSeconds) {}

    /** Deadline aplicado a toda chamada gRPC (GRPC_DEADLINE). */
    @Validated
    @ConfigurationProperties("app.grpc")
    public record Grpc(@NotNull Duration deadline) {}
}
