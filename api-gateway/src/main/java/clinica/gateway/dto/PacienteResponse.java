package clinica.gateway.dto;

public record PacienteResponse(
        long id,
        String nome,
        String cpf,
        String telefone,
        String email,
        String criadoEm) {
}
