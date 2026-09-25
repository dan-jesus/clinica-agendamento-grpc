package clinica.gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PacienteRequest(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter até 150 caracteres.")
        String nome,
        @NotBlank(message = "CPF é obrigatório.")
        @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 dígitos.")
        String cpf,
        @Size(max = 30, message = "Telefone deve ter até 30 caracteres.")
        String telefone,
        @Email(message = "E-mail inválido.")
        @Size(max = 180, message = "E-mail deve ter até 180 caracteres.")
        String email) {
}
