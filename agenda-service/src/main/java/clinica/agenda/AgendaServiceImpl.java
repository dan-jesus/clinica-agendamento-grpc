package clinica.agenda;

import clinica.grpc.agenda.AgendaServiceGrpc;
import clinica.grpc.agenda.AgendarConsultaRequest;
import clinica.grpc.agenda.ConsultarDisponibilidadeRequest;
import clinica.grpc.agenda.ConsultarDisponibilidadeResponse;
import clinica.grpc.agenda.ListarConsultasRequest;
import clinica.grpc.agenda.ListarConsultasResponse;
import clinica.grpc.agenda.Procedimento;
import clinica.grpc.paciente.BuscarPacienteRequest;
import clinica.grpc.paciente.Paciente;
import clinica.grpc.paciente.PacienteServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class AgendaServiceImpl extends AgendaServiceGrpc.AgendaServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(AgendaServiceImpl.class);

    // Grade fixa de horários (segunda a sábado).
    private static final List<String> GRADE = List.of("09:00", "10:00", "11:00", "14:00", "15:00", "16:00", "17:00");

    private final ConsultaRepository repository;
    private final PacienteServiceGrpc.PacienteServiceBlockingStub pacientes;

    public AgendaServiceImpl(ConsultaRepository repository,
                             PacienteServiceGrpc.PacienteServiceBlockingStub pacientes) {
        this.repository = repository;
        this.pacientes = pacientes;
    }

    @Override
    public void consultarDisponibilidade(ConsultarDisponibilidadeRequest request,
                                         StreamObserver<ConsultarDisponibilidadeResponse> out) {
        log.info("ConsultarDisponibilidade data={}", request.getData());
        responder("ConsultarDisponibilidade", out, () -> {
            LocalDate data = lerDataNaoPassada(request.getData());
            Set<String> ocupados = repository.findByDataOrderByHorarioAsc(data).stream()
                    .map(Consulta::getHorario)
                    .collect(Collectors.toSet());
            List<String> livres = horariosDoDia(data).stream().filter(h -> !ocupados.contains(h)).toList();
            return ConsultarDisponibilidadeResponse.newBuilder()
                    .setData(data.toString())
                    .addAllHorariosLivres(livres)
                    .build();
        });
    }

    @Override
    public void agendarConsulta(AgendarConsultaRequest request, StreamObserver<clinica.grpc.agenda.Consulta> out) {
        log.info("AgendarConsulta pacienteId={} procedimento={} data={} horario={}",
                request.getPacienteId(), request.getProcedimento(), request.getData(), request.getHorario());
        responder("AgendarConsulta", out, () -> {
            // 1. Valida os dados recebidos.
            if (request.getPacienteId() <= 0) {
                throw invalido("Informe um paciente válido.");
            }
            Procedimento procedimento = request.getProcedimento();
            if (procedimento == Procedimento.PROCEDIMENTO_NAO_INFORMADO || procedimento == Procedimento.UNRECOGNIZED) {
                throw invalido("Informe um procedimento válido.");
            }
            LocalDate data = lerDataNaoPassada(request.getData());
            String horario = request.getHorario();
            if (!horariosDoDia(data).contains(horario)) {
                throw invalido("Horário \"" + horario + "\" indisponível em " + data
                        + ". A clínica atende de segunda a sábado nos horários " + String.join(", ", GRADE) + ".");
            }
            if (repository.existsByDataAndHorario(data, horario)) {
                throw ocupado(data, horario);
            }

            // 2. Confirma o paciente no paciente-service (comunicação entre microsserviços).
            Paciente paciente = buscarPaciente(request.getPacienteId());

            // 3. Grava; a restrição única (data, horario) protege contra agendamentos concorrentes.
            Consulta consulta = new Consulta(gerarProtocolo(), paciente.getId(), paciente.getNome(),
                    procedimento, data, horario);
            try {
                return repository.saveAndFlush(consulta).toGrpc();
            } catch (DataIntegrityViolationException e) {
                throw ocupado(data, horario);
            }
        });
    }

    @Override
    public void listarConsultas(ListarConsultasRequest request, StreamObserver<ListarConsultasResponse> out) {
        log.info("ListarConsultas data={}", request.getData().isBlank() ? "(todas)" : request.getData());
        responder("ListarConsultas", out, () -> {
            List<Consulta> consultas = request.getData().isBlank()
                    ? repository.findAllByOrderByDataAscHorarioAsc()
                    : repository.findByDataOrderByHorarioAsc(lerData(request.getData()));
            ListarConsultasResponse.Builder resposta = ListarConsultasResponse.newBuilder();
            consultas.forEach(c -> resposta.addConsultas(c.toGrpc()));
            return resposta.build();
        });
    }

    // Chama PacienteService.BuscarPaciente com deadline de 2s e traduz os erros.
    private Paciente buscarPaciente(long id) {
        try {
            return pacientes.withDeadlineAfter(2, TimeUnit.SECONDS)
                    .buscarPaciente(BuscarPacienteRequest.newBuilder().setId(id).build());
        } catch (StatusRuntimeException e) {
            log.warn("BuscarPaciente id={} falhou: {}", id, e.getStatus());
            throw switch (e.getStatus().getCode()) {
                case NOT_FOUND -> Status.NOT_FOUND
                        .withDescription("Paciente " + id + " não encontrado.").asRuntimeException();
                case UNAVAILABLE -> Status.UNAVAILABLE
                        .withDescription("Serviço de pacientes indisponível. Tente novamente mais tarde.")
                        .asRuntimeException();
                case DEADLINE_EXCEEDED -> Status.DEADLINE_EXCEEDED
                        .withDescription("O serviço de pacientes não respondeu a tempo.").asRuntimeException();
                default -> Status.INTERNAL
                        .withDescription("Falha ao consultar o serviço de pacientes.").asRuntimeException();
            };
        }
    }

    // Horários possíveis no dia: nenhum aos domingos; para hoje, só os que ainda não passaram.
    private List<String> horariosDoDia(LocalDate data) {
        if (data.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return List.of();
        }
        if (data.equals(LocalDate.now())) {
            LocalTime agora = LocalTime.now();
            return GRADE.stream().filter(h -> LocalTime.parse(h).isAfter(agora)).toList();
        }
        return GRADE;
    }

    private LocalDate lerDataNaoPassada(String texto) {
        LocalDate data = lerData(texto);
        if (data.isBefore(LocalDate.now())) {
            throw invalido("A data " + data + " já passou.");
        }
        return data;
    }

    private LocalDate lerData(String texto) {
        if (texto == null || texto.isBlank()) {
            throw invalido("Informe a data no formato yyyy-MM-dd.");
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw invalido("Data inválida: \"" + texto + "\". Use o formato yyyy-MM-dd.");
        }
    }

    private static String gerarProtocolo() {
        return "AGD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private static StatusRuntimeException invalido(String mensagem) {
        return Status.INVALID_ARGUMENT.withDescription(mensagem).asRuntimeException();
    }

    private static StatusRuntimeException ocupado(LocalDate data, String horario) {
        return Status.ALREADY_EXISTS
                .withDescription("O horário " + horario + " de " + data + " já está ocupado.")
                .asRuntimeException();
    }

    // Executa a operação, envia a resposta e registra o resultado no log.
    private <T> void responder(String rpc, StreamObserver<T> out, Supplier<T> operacao) {
        try {
            T resposta = operacao.get();
            out.onNext(resposta);
            out.onCompleted();
            log.info("{} -> OK", rpc);
        } catch (StatusRuntimeException e) {
            log.info("{} -> {}: {}", rpc, e.getStatus().getCode(), e.getStatus().getDescription());
            out.onError(e);
        } catch (Exception e) {
            log.error("{} -> falha interna", rpc, e);
            out.onError(Status.INTERNAL.withDescription("Falha interna na agenda.").asRuntimeException());
        }
    }
}
