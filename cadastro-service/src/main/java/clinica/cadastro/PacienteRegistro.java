package clinica.cadastro;

import java.time.LocalDateTime;

public record PacienteRegistro(
        long id,
        String nome,
        String cpf,
        String telefone,
        String email,
        LocalDateTime criadoEm) {
}
