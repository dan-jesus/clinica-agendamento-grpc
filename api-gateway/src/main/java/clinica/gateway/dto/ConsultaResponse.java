package clinica.gateway.dto;

import java.util.List;

public record ConsultaResponse(
        long id,
        boolean confirmado,
        String protocolo,
        String mensagem,
        List<String> horariosAlternativos) {
}
