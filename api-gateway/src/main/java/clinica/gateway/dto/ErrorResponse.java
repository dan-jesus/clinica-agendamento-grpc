package clinica.gateway.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        Map<String, String> campos) {
    public static ErrorResponse simples(int status, String erro, String mensagem) {
        return new ErrorResponse(Instant.now(), status, erro, mensagem, Map.of());
    }
}
