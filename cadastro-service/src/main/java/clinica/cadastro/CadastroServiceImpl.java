package clinica.cadastro;

import clinica.grpc.AutenticarRequest;
import clinica.grpc.AutenticarResponse;
import clinica.grpc.AtualizarPacienteRequest;
import clinica.grpc.BuscarPacienteRequest;
import clinica.grpc.CadastrarPacienteRequest;
import clinica.grpc.CadastroServiceGrpc;
import clinica.grpc.ListarPacientesRequest;
import clinica.grpc.ListarPacientesResponse;
import clinica.grpc.Paciente;
import clinica.grpc.PacienteResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.dao.DataAccessException;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class CadastroServiceImpl extends CadastroServiceGrpc.CadastroServiceImplBase {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final CadastroRepository repository;

    public CadastroServiceImpl(CadastroRepository repository) {
        this.repository = repository;
    }

    @Override
    public void autenticar(AutenticarRequest request, StreamObserver<AutenticarResponse> responseObserver) {
        try {
            Optional<UsuarioRegistro> usuario = repository.buscarUsuarioPorEmail(request.getEmail().trim());
            boolean valido = usuario.isPresent() && PasswordHasher.matches(request.getSenha(), usuario.get().senhaHash());
            AutenticarResponse.Builder resposta = AutenticarResponse.newBuilder().setAutenticado(valido);
            if (valido) {
                UsuarioRegistro u = usuario.get();
                resposta.setUsuarioId(u.id()).setNome(u.nome()).setEmail(u.email()).setMensagem("Autenticado com sucesso.");
            } else {
                resposta.setMensagem("E-mail ou senha inválidos.");
            }
            responseObserver.onNext(resposta.build());
            responseObserver.onCompleted();
        } catch (DataAccessException ex) {
            falhaBanco(responseObserver, ex);
        }
    }

    @Override
    public void cadastrarPaciente(CadastrarPacienteRequest request, StreamObserver<PacienteResponse> responseObserver) {
        try {
            PacienteValidator.validar(request.getNome(), request.getCpf());
            PacienteRegistro paciente = repository.inserirPaciente(
                    request.getNome().trim(), request.getCpf().trim(), request.getTelefone(), request.getEmail());
            responseObserver.onNext(PacienteResponse.newBuilder()
                    .setSucesso(true)
                    .setMensagem("Paciente cadastrado no PostgreSQL.")
                    .setPaciente(toProto(paciente))
                    .build());
            responseObserver.onCompleted();
        } catch (PacienteInvalidoException ex) {
            responseObserver.onNext(PacienteResponse.newBuilder()
                    .setSucesso(false)
                    .setMensagem(mensagemDeErros(ex))
                    .build());
            responseObserver.onCompleted();
        } catch (PacienteJaExisteException ex) {
            responseObserver.onNext(PacienteResponse.newBuilder()
                    .setSucesso(false)
                    .setMensagem(ex.getMessage())
                    .build());
            responseObserver.onCompleted();
        } catch (DataAccessException ex) {
            falhaBanco(responseObserver, ex);
        }
    }

    @Override
    public void atualizarPaciente(AtualizarPacienteRequest request, StreamObserver<PacienteResponse> responseObserver) {
        try {
            if (request.getId() <= 0) {
                responseObserver.onNext(PacienteResponse.newBuilder()
                        .setSucesso(false)
                        .setMensagem("ID do paciente é obrigatório.")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            PacienteValidator.validar(request.getNome(), request.getCpf());
            Optional<PacienteRegistro> atualizado = repository.atualizarPaciente(
                    request.getId(), request.getNome().trim(), request.getCpf().trim(),
                    request.getTelefone(), request.getEmail());
            PacienteResponse.Builder resposta = PacienteResponse.newBuilder();
            if (atualizado.isPresent()) {
                resposta.setSucesso(true).setMensagem("Paciente atualizado no PostgreSQL.")
                        .setPaciente(toProto(atualizado.get()));
            } else {
                resposta.setSucesso(false).setMensagem("Paciente não encontrado.");
            }
            responseObserver.onNext(resposta.build());
            responseObserver.onCompleted();
        } catch (PacienteInvalidoException ex) {
            responseObserver.onNext(PacienteResponse.newBuilder()
                    .setSucesso(false).setMensagem(mensagemDeErros(ex)).build());
            responseObserver.onCompleted();
        } catch (PacienteJaExisteException ex) {
            responseObserver.onNext(PacienteResponse.newBuilder()
                    .setSucesso(false).setMensagem(ex.getMessage()).build());
            responseObserver.onCompleted();
        } catch (DataAccessException ex) {
            falhaBanco(responseObserver, ex);
        }
    }

    @Override
    public void buscarPaciente(BuscarPacienteRequest request, StreamObserver<PacienteResponse> responseObserver) {
        try {
            Optional<PacienteRegistro> paciente = repository.buscarPaciente(request.getId());
            PacienteResponse.Builder resposta = PacienteResponse.newBuilder();
            if (paciente.isPresent()) {
                resposta.setSucesso(true).setMensagem("Paciente encontrado.").setPaciente(toProto(paciente.get()));
            } else {
                resposta.setSucesso(false).setMensagem("Paciente não encontrado.");
            }
            responseObserver.onNext(resposta.build());
            responseObserver.onCompleted();
        } catch (DataAccessException ex) {
            falhaBanco(responseObserver, ex);
        }
    }

    @Override
    public void listarPacientes(ListarPacientesRequest request, StreamObserver<ListarPacientesResponse> responseObserver) {
        try {
            ListarPacientesResponse.Builder resposta = ListarPacientesResponse.newBuilder();
            for (PacienteRegistro paciente : repository.listarPacientes()) {
                resposta.addPacientes(toProto(paciente));
            }
            responseObserver.onNext(resposta.build());
            responseObserver.onCompleted();
        } catch (DataAccessException ex) {
            falhaBanco(responseObserver, ex);
        }
    }

    private static String mensagemDeErros(PacienteInvalidoException ex) {
        return String.join("; ", ex.getErros().values());
    }

    private static Paciente toProto(PacienteRegistro paciente) {
        Paciente.Builder builder = Paciente.newBuilder()
                .setId(paciente.id())
                .setNome(paciente.nome())
                .setCpf(paciente.cpf())
                .setCriadoEm(paciente.criadoEm().format(ISO));
        if (paciente.telefone() != null) builder.setTelefone(paciente.telefone());
        if (paciente.email() != null) builder.setEmail(paciente.email());
        return builder.build();
    }

    private static void falhaBanco(StreamObserver<?> observer, DataAccessException ex) {
        ex.printStackTrace();
        observer.onError(Status.INTERNAL
                .withDescription("Falha ao acessar o banco de cadastro.")
                .withCause(ex)
                .asRuntimeException());
    }
}
