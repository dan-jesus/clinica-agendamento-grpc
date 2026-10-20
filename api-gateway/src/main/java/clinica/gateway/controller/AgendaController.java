package clinica.gateway.controller;

import clinica.gateway.config.AppProperties;
import clinica.gateway.dto.ConsultaRequest;
import clinica.gateway.dto.ConsultaResponse;
import clinica.gateway.dto.DisponibilidadeResponse;
import clinica.gateway.mapper.ConsultaMapper;
import clinica.grpc.agenda.AgendaServiceGrpc.AgendaServiceBlockingStub;
import clinica.grpc.agenda.ConsultarDisponibilidadeRequest;
import clinica.grpc.agenda.ListarConsultasRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** REST de disponibilidade e consultas -> AgendaService (gRPC). */
@RestController
@RequestMapping("/api/v1")
public class AgendaController {

    private final AgendaServiceBlockingStub stub;
    private final AppProperties.Grpc grpc;

    public AgendaController(AgendaServiceBlockingStub stub, AppProperties.Grpc grpc) {
        this.stub = stub;
        this.grpc = grpc;
    }

    private AgendaServiceBlockingStub comDeadline() {
        return stub.withDeadlineAfter(grpc.deadline().toMillis(), TimeUnit.MILLISECONDS);
    }

    @GetMapping("/agenda/disponibilidade")
    public DisponibilidadeResponse disponibilidade(@RequestParam String data) {
        validarData(data);
        return ConsultaMapper.toDto(comDeadline()
                .consultarDisponibilidade(ConsultarDisponibilidadeRequest.newBuilder().setData(data).build()));
    }

    @GetMapping("/consultas")
    public List<ConsultaResponse> listar(@RequestParam(required = false) String data) {
        ListarConsultasRequest.Builder req = ListarConsultasRequest.newBuilder();
        if (data != null && !data.isBlank()) {
            validarData(data);
            req.setData(data);
        }
        return comDeadline().listarConsultas(req.build())
                .getConsultasList().stream().map(ConsultaMapper::toDto).toList();
    }

    @PostMapping("/consultas")
    public ResponseEntity<ConsultaResponse> agendar(@Valid @RequestBody ConsultaRequest req) {
        ConsultaResponse criada = ConsultaMapper.toDto(comDeadline().agendarConsulta(ConsultaMapper.toProto(req)));
        // Não há rota GET /consultas/{id}; por isso 201 sem header Location.
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    /** O parâmetro de query "data" precisa ser uma data ISO válida (yyyy-MM-dd). */
    private static void validarData(String data) {
        try {
            LocalDate.parse(data);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parâmetro data inválido; use yyyy-MM-dd.");
        }
    }
}
