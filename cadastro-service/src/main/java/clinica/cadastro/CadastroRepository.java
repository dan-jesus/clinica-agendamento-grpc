package clinica.cadastro;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Camada de acesso a dados de usuários e pacientes, agora apoiada em Spring
 * Data JPA (UsuarioRepository / PacienteRepository) em vez de JDBC manual.
 * Mantém a mesma API usada por CadastroServiceImpl para minimizar o impacto
 * na camada gRPC.
 */
@Service
public class CadastroRepository {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;

    public CadastroRepository(UsuarioRepository usuarioRepository, PacienteRepository pacienteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
    }

    public void garantirUsuarioInicial(String nome, String email, String senha) {
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }
        try {
            usuarioRepository.save(new Usuario(nome, email.toLowerCase(), PasswordHasher.hash(senha)));
        } catch (DataIntegrityViolationException ignorado) {
            // usuário já existe (corrida entre múltiplas inicializações); nada a fazer
        }
    }

    public Optional<UsuarioRegistro> buscarUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email).map(CadastroRepository::toRegistro);
    }

    @Transactional
    public PacienteRegistro inserirPaciente(String nome, String cpf, String telefone, String email) {
        if (pacienteRepository.existsByCpf(cpf)) {
            throw new PacienteJaExisteException("Já existe um paciente com esse CPF.");
        }
        try {
            PacienteEntity salvo = pacienteRepository.save(
                    new PacienteEntity(nome, cpf, vazioParaNulo(telefone), vazioParaNulo(email)));
            return toRegistro(salvo);
        } catch (DataIntegrityViolationException ex) {
            throw new PacienteJaExisteException("Já existe um paciente com esse CPF.", ex);
        }
    }

    @Transactional
    public Optional<PacienteRegistro> atualizarPaciente(long id, String nome, String cpf, String telefone, String email) {
        Optional<PacienteEntity> existente = pacienteRepository.findById(id);
        if (existente.isEmpty()) {
            return Optional.empty();
        }
        PacienteEntity paciente = existente.get();
        paciente.setNome(nome);
        paciente.setCpf(cpf);
        paciente.setTelefone(vazioParaNulo(telefone));
        paciente.setEmail(vazioParaNulo(email));
        try {
            return Optional.of(toRegistro(pacienteRepository.save(paciente)));
        } catch (DataIntegrityViolationException ex) {
            throw new PacienteJaExisteException("Já existe outro paciente com esse CPF.", ex);
        }
    }

    public Optional<PacienteRegistro> buscarPaciente(long id) {
        return pacienteRepository.findById(id).map(CadastroRepository::toRegistro);
    }

    public List<PacienteRegistro> listarPacientes() {
        return pacienteRepository.findAllByOrderByNomeAscIdAsc().stream()
                .map(CadastroRepository::toRegistro)
                .toList();
    }

    private static PacienteRegistro toRegistro(PacienteEntity entity) {
        return new PacienteRegistro(
                entity.getId(), entity.getNome(), entity.getCpf(),
                entity.getTelefone(), entity.getEmail(), entity.getCriadoEm());
    }

    private static UsuarioRegistro toRegistro(Usuario usuario) {
        return new UsuarioRegistro(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getSenhaHash());
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
