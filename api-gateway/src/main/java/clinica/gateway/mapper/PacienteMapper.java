package clinica.gateway.mapper;

import clinica.gateway.dto.PacienteRequest;
import clinica.gateway.dto.PacienteResponse;
import clinica.grpc.paciente.CadastrarPacienteRequest;
import clinica.grpc.paciente.Paciente;

/** Tradução JSON (DTO) <-> Protobuf para pacientes. */
public final class PacienteMapper {

    private PacienteMapper() {}

    public static CadastrarPacienteRequest toProto(PacienteRequest r) {
        return CadastrarPacienteRequest.newBuilder()
                .setNome(r.nome().trim())
                .setCpf(PacienteRequest.somenteDigitos(r.cpf()))
                .setTelefone(PacienteRequest.somenteDigitos(r.telefone()))
                .setEmail(r.email() == null ? "" : r.email().trim()) // proto3 não aceita null
                .build();
    }

    public static PacienteResponse toDto(Paciente p) {
        return new PacienteResponse(p.getId(), p.getNome(), p.getCpf(), p.getTelefone(),
                p.getEmail().isEmpty() ? null : p.getEmail(), p.getCriadoEm());
    }
}
