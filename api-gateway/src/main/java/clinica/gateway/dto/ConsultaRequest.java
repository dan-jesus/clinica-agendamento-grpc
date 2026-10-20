package clinica.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Agendamento de consulta (regras de grade e data no passado ficam no agenda-service). */
public record ConsultaRequest(
        @NotNull(message = "pacienteId é obrigatório")
        @Positive(message = "pacienteId deve ser positivo") Long pacienteId,
        @NotBlank(message = "procedimento é obrigatório")
        @Pattern(regexp = "LIMPEZA|RESTAURACAO|CANAL|EXTRACAO|CLAREAMENTO",
                 message = "procedimento deve ser LIMPEZA, RESTAURACAO, CANAL, EXTRACAO ou CLAREAMENTO") String procedimento,
        @NotBlank(message = "data é obrigatória")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "data deve estar no formato yyyy-MM-dd") String data,
        @NotBlank(message = "horario é obrigatório")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "horario deve estar no formato HH:mm") String horario) {}
