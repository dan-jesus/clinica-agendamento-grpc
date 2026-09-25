package clinica.agenda;

import clinica.grpc.Procedimento;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Classe de validação dos dados de uma solicitação de agendamento, equivalente
 * em papel à validação por anotações (@NotBlank/@NotNull) do projeto de
 * referência, porém aplicada manualmente porque este serviço expõe uma API
 * gRPC (sem o disparo automático de validação do Spring MVC).
 */
public final class AgendamentoValidator {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private AgendamentoValidator() {
    }

    public record AgendamentoValidado(LocalDate data, LocalTime horario) {
    }

    /**
     * Valida os campos de uma solicitação de agendamento.
     *
     * @throws ConsultaInvalidaException se algum campo for inválido, contendo em
     *                                   {@link ConsultaInvalidaException#getErros()} um erro por campo.
     */
    public static AgendamentoValidado validar(String paciente, Procedimento procedimento, String data, String horario) {
        Map<String, String> erros = new HashMap<>();

        if (paciente == null || paciente.isBlank()) {
            erros.put("paciente", "nome do paciente não informado");
        }
        if (procedimento == null
                || procedimento == Procedimento.PROCEDIMENTO_NAO_INFORMADO
                || procedimento == Procedimento.UNRECOGNIZED) {
            erros.put("procedimento", "procedimento não informado");
        }

        LocalDate dataConvertida = null;
        try {
            dataConvertida = LocalDate.parse(data, FORMATO_DATA);
        } catch (DateTimeParseException | NullPointerException ex) {
            erros.put("data", "data inválida, use o formato dd/MM/aaaa");
        }

        LocalTime horarioConvertido = null;
        try {
            horarioConvertido = LocalTime.parse(horario, FORMATO_HORA);
        } catch (DateTimeParseException | NullPointerException ex) {
            erros.put("horario", "horário inválido, use o formato HH:mm");
        }

        if (!erros.isEmpty()) {
            throw new ConsultaInvalidaException("Dados da consulta inválidos.", erros);
        }

        return new AgendamentoValidado(dataConvertida, horarioConvertido);
    }

    public static Optional<LocalDate> parseData(String data) {
        try {
            return Optional.of(LocalDate.parse(data, FORMATO_DATA));
        } catch (DateTimeParseException | NullPointerException ex) {
            return Optional.empty();
        }
    }

    public static String formatarData(LocalDate data) {
        return data.format(FORMATO_DATA);
    }

    public static String formatarHorario(LocalTime horario) {
        return horario.format(FORMATO_HORA);
    }
}
