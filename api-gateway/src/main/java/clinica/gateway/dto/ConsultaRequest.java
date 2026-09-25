package clinica.gateway.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConsultaRequest(
        @NotNull(message = "Paciente é obrigatório.")
        @Positive(message = "Paciente inválido.")
        Long pacienteId,
        @NotBlank(message = "Procedimento é obrigatório.")
        String procedimento,
        @NotNull(message = "Data é obrigatória.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate data,
        @NotNull(message = "Horário é obrigatório.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime horario) {
}
