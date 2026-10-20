package clinica.gateway.mapper;

import clinica.gateway.dto.ConsultaRequest;
import clinica.gateway.dto.ConsultaResponse;
import clinica.gateway.dto.DisponibilidadeResponse;
import clinica.grpc.agenda.AgendarConsultaRequest;
import clinica.grpc.agenda.Consulta;
import clinica.grpc.agenda.ConsultarDisponibilidadeResponse;
import clinica.grpc.agenda.Procedimento;

/** Tradução JSON (DTO) <-> Protobuf para agenda e consultas. */
public final class ConsultaMapper {

    private ConsultaMapper() {}

    public static AgendarConsultaRequest toProto(ConsultaRequest r) {
        return AgendarConsultaRequest.newBuilder()
                .setPacienteId(r.pacienteId())
                .setProcedimento(Procedimento.valueOf(r.procedimento())) // valor já validado no DTO
                .setData(r.data())
                .setHorario(r.horario())
                .build();
    }

    public static ConsultaResponse toDto(Consulta c) {
        return new ConsultaResponse(c.getId(), c.getProtocolo(), c.getPacienteId(), c.getPacienteNome(),
                c.getProcedimento().name(), c.getData(), c.getHorario(), c.getCriadoEm());
    }

    public static DisponibilidadeResponse toDto(ConsultarDisponibilidadeResponse r) {
        return new DisponibilidadeResponse(r.getData(), r.getHorariosLivresList());
    }
}
