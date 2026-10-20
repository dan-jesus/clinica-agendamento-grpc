package clinica.paciente;

import clinica.grpc.paciente.BuscarPacienteRequest;
import clinica.grpc.paciente.CadastrarPacienteRequest;
import clinica.grpc.paciente.ListarPacientesRequest;
import clinica.grpc.paciente.ListarPacientesResponse;
import clinica.grpc.paciente.PacienteServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class PacienteGrpcService extends PacienteServiceGrpc.PacienteServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(PacienteGrpcService.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final PacienteRepository repository;

    public PacienteGrpcService(PacienteRepository repository) {
        this.repository = repository;
    }

    @Override
    public void cadastrarPaciente(CadastrarPacienteRequest req,
                                  StreamObserver<clinica.grpc.paciente.Paciente> out) {
        log.info("CadastrarPaciente nome='{}'", req.getNome());

        String nome = req.getNome().trim();
        String email = req.getEmail().trim();

        String erro = null;
        if (nome.length() < 2 || nome.length() > 120) {
            erro = "O nome é obrigatório e deve ter entre 2 e 120 caracteres.";
        } else if (!req.getCpf().matches("\\d{11}")) {
            erro = "O CPF é obrigatório e deve ter exatamente 11 dígitos.";
        } else if (!req.getTelefone().matches("\\d{10,11}")) {
            erro = "O telefone é obrigatório e deve ter 10 ou 11 dígitos.";
        } else if (!email.isEmpty() && !EMAIL.matcher(email).matches()) {
            erro = "O e-mail informado é inválido.";
        }
        if (erro != null) {
            out.onError(Status.INVALID_ARGUMENT.withDescription(erro).asRuntimeException());
            return;
        }

        if (repository.existsByCpf(req.getCpf())) {
            out.onError(cpfDuplicado());
            return;
        }

        try {
            Paciente salvo = repository.saveAndFlush(
                    new Paciente(nome, req.getCpf(), req.getTelefone(), email.isEmpty() ? null : email));
            out.onNext(paraProto(salvo));
            out.onCompleted();
        } catch (DataIntegrityViolationException e) {
            // Cadastros simultâneos com o mesmo CPF: quem perde a corrida cai na restrição única do banco.
            out.onError(cpfDuplicado());
        }
    }

    @Override
    public void buscarPaciente(BuscarPacienteRequest req,
                               StreamObserver<clinica.grpc.paciente.Paciente> out) {
        log.info("BuscarPaciente id={}", req.getId());
        repository.findById(req.getId()).ifPresentOrElse(
                p -> {
                    out.onNext(paraProto(p));
                    out.onCompleted();
                },
                () -> out.onError(Status.NOT_FOUND
                        .withDescription("Paciente não encontrado.").asRuntimeException()));
    }

    @Override
    public void listarPacientes(ListarPacientesRequest req,
                                StreamObserver<ListarPacientesResponse> out) {
        log.info("ListarPacientes");
        ListarPacientesResponse.Builder resposta = ListarPacientesResponse.newBuilder();
        repository.findAllByOrderByNomeAsc().forEach(p -> resposta.addPacientes(paraProto(p)));
        out.onNext(resposta.build());
        out.onCompleted();
    }

    private static io.grpc.StatusRuntimeException cpfDuplicado() {
        return Status.ALREADY_EXISTS.withDescription("Já existe um paciente cadastrado com este CPF.")
                .asRuntimeException();
    }

    private static clinica.grpc.paciente.Paciente paraProto(Paciente p) {
        return clinica.grpc.paciente.Paciente.newBuilder()
                .setId(p.getId())
                .setNome(p.getNome())
                .setCpf(p.getCpf())
                .setTelefone(p.getTelefone())
                .setEmail(p.getEmail() == null ? "" : p.getEmail())
                .setCriadoEm(p.getCriadoEm().toString())
                .build();
    }
}
