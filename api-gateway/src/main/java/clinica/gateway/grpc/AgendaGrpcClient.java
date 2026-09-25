package clinica.gateway.grpc;

import clinica.gateway.dto.ConsultaItemResponse;
import clinica.gateway.dto.ConsultaRequest;
import clinica.gateway.dto.ConsultaResponse;
import clinica.gateway.dto.DisponibilidadeResponse;
import clinica.grpc.AgendaServiceGrpc;
import clinica.grpc.AgendarConsultaRequest;
import clinica.grpc.AgendarConsultaResponse;
import clinica.grpc.Consulta;
import clinica.grpc.ConsultarDisponibilidadeRequest;
import clinica.grpc.ListarConsultasRequest;
import clinica.grpc.Procedimento;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AgendaGrpcClient {
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${app.grpc.agenda-host}") private String host;
    @Value("${app.grpc.agenda-port}") private int port;

    private ManagedChannel channel;
    private AgendaServiceGrpc.AgendaServiceBlockingStub stub;

    @PostConstruct
    void iniciar() {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = AgendaServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    void encerrar() {
        if (channel != null) channel.shutdown();
    }

    public DisponibilidadeResponse disponibilidade(LocalDate data) {
        String dataBr = data.format(DATA_BR);
        var resposta = chamada().consultarDisponibilidade(
                ConsultarDisponibilidadeRequest.newBuilder().setData(dataBr).build());
        return new DisponibilidadeResponse(data.toString(), resposta.getHorariosLivresList());
    }

    public ConsultaResponse agendar(ConsultaRequest request, long pacienteId, String pacienteNome) {
        Procedimento procedimento = Procedimento.valueOf(request.procedimento().trim().toUpperCase());
        AgendarConsultaResponse resposta = chamada().agendarConsulta(AgendarConsultaRequest.newBuilder()
                .setPacienteId(pacienteId)
                .setPaciente(pacienteNome)
                .setProcedimento(procedimento)
                .setData(request.data().format(DATA_BR))
                .setHorario(request.horario().withSecond(0).withNano(0).toString())
                .build());
        return new ConsultaResponse(
                resposta.getConsultaId(),
                resposta.getConfirmado(),
                resposta.getProtocolo(),
                resposta.getMensagem(),
                resposta.getHorariosAlternativosList());
    }

    public List<ConsultaItemResponse> listar(LocalDate data) {
        ListarConsultasRequest.Builder request = ListarConsultasRequest.newBuilder();
        if (data != null) request.setData(data.format(DATA_BR));
        return chamada().listarConsultas(request.build()).getConsultasList().stream()
                .map(AgendaGrpcClient::toDto)
                .toList();
    }

    private AgendaServiceGrpc.AgendaServiceBlockingStub chamada() {
        return stub.withDeadlineAfter(5, TimeUnit.SECONDS);
    }

    private static ConsultaItemResponse toDto(Consulta c) {
        LocalDate data = LocalDate.parse(c.getData(), DATA_BR);
        return new ConsultaItemResponse(
                c.getId(), c.getProtocolo(), c.getPacienteId(), c.getPaciente(),
                c.getProcedimento().name(), data.toString(), c.getHorario(), c.getCriadoEm());
    }
}
