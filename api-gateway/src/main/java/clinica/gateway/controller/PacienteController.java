package clinica.gateway.controller;

import clinica.gateway.config.AppProperties;
import clinica.gateway.dto.PacienteRequest;
import clinica.gateway.dto.PacienteResponse;
import clinica.gateway.mapper.PacienteMapper;
import clinica.grpc.paciente.BuscarPacienteRequest;
import clinica.grpc.paciente.ListarPacientesRequest;
import clinica.grpc.paciente.PacienteServiceGrpc.PacienteServiceBlockingStub;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** REST de pacientes -> PacienteService (gRPC). */
@RestController
@RequestMapping("/api/v1/pacientes")
public class PacienteController {

    private final PacienteServiceBlockingStub stub;
    private final AppProperties.Grpc grpc;

    public PacienteController(PacienteServiceBlockingStub stub, AppProperties.Grpc grpc) {
        this.stub = stub;
        this.grpc = grpc;
    }

    /** Deadline novo a cada chamada (o deadline é absoluto, não pode ser reaproveitado). */
    private PacienteServiceBlockingStub comDeadline() {
        return stub.withDeadlineAfter(grpc.deadline().toMillis(), TimeUnit.MILLISECONDS);
    }

    @GetMapping
    public List<PacienteResponse> listar() {
        return comDeadline().listarPacientes(ListarPacientesRequest.getDefaultInstance())
                .getPacientesList().stream().map(PacienteMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public PacienteResponse buscar(@PathVariable long id) {
        return PacienteMapper.toDto(comDeadline().buscarPaciente(BuscarPacienteRequest.newBuilder().setId(id).build()));
    }

    @PostMapping
    public ResponseEntity<PacienteResponse> cadastrar(@Valid @RequestBody PacienteRequest req) {
        PacienteResponse criado = PacienteMapper.toDto(comDeadline().cadastrarPaciente(PacienteMapper.toProto(req)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }
}
