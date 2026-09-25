package clinica.gateway.grpc;

import clinica.gateway.dto.PacienteRequest;
import clinica.gateway.dto.PacienteResponse;
import clinica.grpc.AutenticarRequest;
import clinica.grpc.AutenticarResponse;
import clinica.grpc.AtualizarPacienteRequest;
import clinica.grpc.BuscarPacienteRequest;
import clinica.grpc.CadastrarPacienteRequest;
import clinica.grpc.CadastroServiceGrpc;
import clinica.grpc.ListarPacientesRequest;
import clinica.grpc.Paciente;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CadastroGrpcClient {
    @Value("${app.grpc.cadastro-host}") private String host;
    @Value("${app.grpc.cadastro-port}") private int port;

    private ManagedChannel channel;
    private CadastroServiceGrpc.CadastroServiceBlockingStub stub;

    @PostConstruct
    void iniciar() {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = CadastroServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    void encerrar() {
        if (channel != null) channel.shutdown();
    }

    public AutenticarResponse autenticar(String email, String senha) {
        return chamada().autenticar(AutenticarRequest.newBuilder().setEmail(email).setSenha(senha).build());
    }

    public clinica.grpc.PacienteResponse cadastrar(PacienteRequest request) {
        return chamada().cadastrarPaciente(CadastrarPacienteRequest.newBuilder()
                .setNome(request.nome().trim())
                .setCpf(request.cpf().trim())
                .setTelefone(texto(request.telefone()))
                .setEmail(texto(request.email()))
                .build());
    }

    public clinica.grpc.PacienteResponse atualizar(long id, PacienteRequest request) {
        return chamada().atualizarPaciente(AtualizarPacienteRequest.newBuilder()
                .setId(id)
                .setNome(request.nome().trim())
                .setCpf(request.cpf().trim())
                .setTelefone(texto(request.telefone()))
                .setEmail(texto(request.email()))
                .build());
    }

    public clinica.grpc.PacienteResponse buscar(long id) {
        return chamada().buscarPaciente(BuscarPacienteRequest.newBuilder().setId(id).build());
    }

    public List<PacienteResponse> listar() {
        return chamada().listarPacientes(ListarPacientesRequest.newBuilder().build())
                .getPacientesList().stream().map(CadastroGrpcClient::toDto).toList();
    }

    private CadastroServiceGrpc.CadastroServiceBlockingStub chamada() {
        return stub.withDeadlineAfter(5, TimeUnit.SECONDS);
    }

    public static PacienteResponse toDto(Paciente p) {
        return new PacienteResponse(p.getId(), p.getNome(), p.getCpf(),
                vazioParaNulo(p.getTelefone()), vazioParaNulo(p.getEmail()), p.getCriadoEm());
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
