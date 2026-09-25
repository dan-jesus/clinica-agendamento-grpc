package clinica.gateway.dto;

import java.util.List;

public record DisponibilidadeResponse(String data, List<String> horariosLivres) {
}
