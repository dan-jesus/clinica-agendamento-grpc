package clinica.gateway.dto;

public record ConsultaItemResponse(
        long id,
        String protocolo,
        long pacienteId,
        String paciente,
        String procedimento,
        String data,
        String horario,
        String criadoEm) {
}
