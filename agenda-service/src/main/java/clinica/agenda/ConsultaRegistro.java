package clinica.agenda;

import clinica.grpc.Procedimento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ConsultaRegistro(
        long id,
        String protocolo,
        Long pacienteId,
        String pacienteNome,
        Procedimento procedimento,
        LocalDate data,
        LocalTime horario,
        LocalDateTime criadoEm) {
}
