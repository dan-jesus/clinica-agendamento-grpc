package clinica.agenda;

import clinica.grpc.AgendaServiceGrpc;
import clinica.grpc.AgendarConsultaRequest;
import clinica.grpc.AgendarConsultaResponse;
import clinica.grpc.ConsultarDisponibilidadeRequest;
import clinica.grpc.ConsultarDisponibilidadeResponse;
import clinica.grpc.Procedimento;
import io.grpc.stub.StreamObserver;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AgendaServiceImpl extends AgendaServiceGrpc.AgendaServiceImplBase {

    private static final List<String> HORARIOS_ATENDIMENTO =
            List.of("09:00", "10:00", "11:00", "14:00", "15:00", "16:00", "17:00");

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    // Agenda mantida em memória: data -> horários já reservados.
    private final Map<String, Set<String>> reservas = new ConcurrentHashMap<>();

    @Override
    public void consultarDisponibilidade(ConsultarDisponibilidadeRequest request,
                                         StreamObserver<ConsultarDisponibilidadeResponse> responseObserver) {
        String data = request.getData();
        System.out.println("Disponibilidade consultada para " + data);

        ConsultarDisponibilidadeResponse resposta = ConsultarDisponibilidadeResponse.newBuilder()
                .setData(data)
                .addAllHorariosLivres(dataValida(data) ? horariosLivres(data) : List.of())
                .build();

        responseObserver.onNext(resposta);
        responseObserver.onCompleted();
    }

    @Override
    public void agendarConsulta(AgendarConsultaRequest request,
                                StreamObserver<AgendarConsultaResponse> responseObserver) {
        System.out.printf("Agendamento solicitado: paciente=%s, procedimento=%s, data=%s, horario=%s%n",
                request.getPaciente(), request.getProcedimento(), request.getData(), request.getHorario());

        AgendarConsultaResponse resposta = avaliar(request);
        System.out.println(resposta.getConfirmado()
                ? "Confirmado, protocolo " + resposta.getProtocolo()
                : "Recusado: " + resposta.getMensagem());

        responseObserver.onNext(resposta);
        responseObserver.onCompleted();
    }

    private AgendarConsultaResponse avaliar(AgendarConsultaRequest request) {
        if (request.getPaciente().isBlank()) {
            return recusar("Nome do paciente não informado.", List.of());
        }
        if (request.getProcedimento() == Procedimento.PROCEDIMENTO_NAO_INFORMADO
                || request.getProcedimento() == Procedimento.UNRECOGNIZED) {
            return recusar("Procedimento não informado.", List.of());
        }
        if (!dataValida(request.getData())) {
            return recusar("Data inválida. Use o formato dd/MM/aaaa.", List.of());
        }
        if (!HORARIOS_ATENDIMENTO.contains(request.getHorario())) {
            return recusar("Horário fora do atendimento da clínica.", HORARIOS_ATENDIMENTO);
        }

        // add devolve false quando o horário já estava reservado, garantindo
        // que duas chamadas simultâneas não ocupem o mesmo horário.
        boolean reservado = reservas
                .computeIfAbsent(request.getData(), data -> ConcurrentHashMap.newKeySet())
                .add(request.getHorario());

        if (!reservado) {
            return recusar("Horário já ocupado.", horariosLivres(request.getData()));
        }

        return AgendarConsultaResponse.newBuilder()
                .setConfirmado(true)
                .setProtocolo("AGD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .setMensagem("Consulta confirmada.")
                .build();
    }

    private AgendarConsultaResponse recusar(String mensagem, List<String> alternativas) {
        return AgendarConsultaResponse.newBuilder()
                .setConfirmado(false)
                .setMensagem(mensagem)
                .addAllHorariosAlternativos(alternativas)
                .build();
    }

    private List<String> horariosLivres(String data) {
        Set<String> ocupados = reservas.getOrDefault(data, Set.of());
        return HORARIOS_ATENDIMENTO.stream().filter(horario -> !ocupados.contains(horario)).toList();
    }

    private boolean dataValida(String data) {
        try {
            LocalDate.parse(data, FORMATO_DATA);
            return true;
        } catch (DateTimeParseException excecao) {
            return false;
        }
    }
}
