package clinica.agenda;

import clinica.agenda.AgendamentoValidator.AgendamentoValidado;
import clinica.grpc.AgendaServiceGrpc;
import clinica.grpc.AgendarConsultaRequest;
import clinica.grpc.AgendarConsultaResponse;
import clinica.grpc.Consulta;
import clinica.grpc.ConsultarDisponibilidadeRequest;
import clinica.grpc.ConsultarDisponibilidadeResponse;
import clinica.grpc.ListarConsultasRequest;
import clinica.grpc.ListarConsultasResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.dao.DataAccessException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AgendaServiceImpl extends AgendaServiceGrpc.AgendaServiceImplBase {

    private static final DateTimeFormatter FORMATO_DATA_HORA_ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AgendaRepository repository;

    public AgendaServiceImpl(AgendaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void consultarDisponibilidade(ConsultarDisponibilidadeRequest request,
                                         StreamObserver<ConsultarDisponibilidadeResponse> responseObserver) {
        try {
            Optional<LocalDate> data = AgendamentoValidator.parseData(request.getData());
            List<String> livres = data.isPresent() ? horariosLivres(data.get()) : List.of();
            responseObserver.onNext(ConsultarDisponibilidadeResponse.newBuilder()
                    .setData(request.getData())
                    .addAllHorariosLivres(livres)
                    .build());
            responseObserver.onCompleted();
        } catch (DataAccessException ex) {
            falhaBanco(responseObserver, ex);
        }
    }

    @Override
    public void agendarConsulta(AgendarConsultaRequest request,
                                StreamObserver<AgendarConsultaResponse> responseObserver) {
        try {
            responseObserver.onNext(avaliarEGravar(request));
            responseObserver.onCompleted();
        } catch (ConsultaInvalidaException ex) {
            responseObserver.onNext(recusar(mensagemDeErros(ex), List.of()));
            responseObserver.onCompleted();
        } catch (DataAccessException ex) {
            falhaBanco(responseObserver, ex);
        }
    }

    @Override
    public void listarConsultas(ListarConsultasRequest request,
                                StreamObserver<ListarConsultasResponse> responseObserver) {
        try {
            Optional<LocalDate> filtro = request.getData().isBlank()
                    ? Optional.empty()
                    : AgendamentoValidator.parseData(request.getData());
            if (!request.getData().isBlank() && filtro.isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Data inválida. Use dd/MM/aaaa.")
                        .asRuntimeException());
                return;
            }

            ListarConsultasResponse.Builder resposta = ListarConsultasResponse.newBuilder();
            for (ConsultaRegistro registro : repository.listar(filtro)) {
                resposta.addConsultas(toProto(registro));
            }
            responseObserver.onNext(resposta.build());
            responseObserver.onCompleted();
        } catch (DataAccessException ex) {
            falhaBanco(responseObserver, ex);
        }
    }

    private AgendarConsultaResponse avaliarEGravar(AgendarConsultaRequest request) {
        AgendamentoValidado validado = AgendamentoValidator.validar(
                request.getPaciente(), request.getProcedimento(), request.getData(), request.getHorario());

        List<LocalTime> horariosAtendimento = repository.horariosAtendimento();
        if (!horariosAtendimento.contains(validado.horario())) {
            return recusar("Horário fora do atendimento da clínica.", horariosComoTexto(horariosAtendimento));
        }

        String protocolo = "AGD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Long pacienteId = request.getPacienteId() > 0 ? request.getPacienteId() : null;

        try {
            ConsultaRegistro criada = repository.inserir(
                    protocolo,
                    pacienteId,
                    request.getPaciente().trim(),
                    request.getProcedimento(),
                    validado.data(),
                    validado.horario());

            return AgendarConsultaResponse.newBuilder()
                    .setConfirmado(true)
                    .setConsultaId(criada.id())
                    .setProtocolo(criada.protocolo())
                    .setMensagem("Consulta confirmada e persistida no PostgreSQL.")
                    .build();
        } catch (HorarioIndisponivelException ex) {
            return recusar("Horário já ocupado.", horariosLivres(validado.data()));
        }
    }

    private List<String> horariosLivres(LocalDate data) {
        Set<LocalTime> ocupados = new HashSet<>(repository.horariosOcupados(data));
        return repository.horariosAtendimento().stream()
                .filter(horario -> !ocupados.contains(horario))
                .map(AgendamentoValidator::formatarHorario)
                .toList();
    }

    private static List<String> horariosComoTexto(List<LocalTime> horarios) {
        return horarios.stream().map(AgendamentoValidator::formatarHorario).toList();
    }

    private static String mensagemDeErros(ConsultaInvalidaException ex) {
        return String.join("; ", ex.getErros().values());
    }

    private static AgendarConsultaResponse recusar(String mensagem, List<String> alternativas) {
        return AgendarConsultaResponse.newBuilder()
                .setConfirmado(false)
                .setMensagem(mensagem)
                .addAllHorariosAlternativos(alternativas)
                .build();
    }

    private static Consulta toProto(ConsultaRegistro registro) {
        Consulta.Builder consulta = Consulta.newBuilder()
                .setId(registro.id())
                .setProtocolo(registro.protocolo())
                .setPaciente(registro.pacienteNome())
                .setProcedimento(registro.procedimento())
                .setData(AgendamentoValidator.formatarData(registro.data()))
                .setHorario(AgendamentoValidator.formatarHorario(registro.horario()))
                .setCriadoEm(registro.criadoEm().format(FORMATO_DATA_HORA_ISO));
        if (registro.pacienteId() != null) {
            consulta.setPacienteId(registro.pacienteId());
        }
        return consulta.build();
    }

    private static void falhaBanco(StreamObserver<?> responseObserver, DataAccessException ex) {
        ex.printStackTrace();
        responseObserver.onError(Status.INTERNAL
                .withDescription("Falha ao acessar o banco de dados da agenda.")
                .withCause(ex)
                .asRuntimeException());
    }
}
