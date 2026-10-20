package clinica.gateway.dto;

public record ConsultaResponse(long id, String protocolo, long pacienteId, String pacienteNome,
                               String procedimento, String data, String horario, String criadoEm) {}
