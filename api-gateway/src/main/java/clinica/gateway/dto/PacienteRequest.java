package clinica.gateway.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cadastro de paciente; CPF e telefone aceitam máscara (o Gateway envia só dígitos). */
public record PacienteRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(min = 2, max = 120, message = "nome deve ter entre 2 e 120 caracteres") String nome,
        @NotBlank(message = "cpf é obrigatório") String cpf,
        @NotBlank(message = "telefone é obrigatório") String telefone,
        @Email(message = "email inválido") String email) {

    /** Remove tudo que não for dígito. */
    public static String somenteDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    @AssertTrue(message = "cpf deve ter 11 dígitos")
    public boolean isCpfValido() {
        if (cpf == null || cpf.isBlank()) return true; // já coberto por @NotBlank
        return cpf.matches("[\\d.\\-\\s]+") && somenteDigitos(cpf).length() == 11;
    }

    @AssertTrue(message = "telefone deve ter 10 ou 11 dígitos")
    public boolean isTelefoneValido() {
        if (telefone == null || telefone.isBlank()) return true;
        int n = somenteDigitos(telefone).length();
        return telefone.matches("[\\d()\\-\\s+]+") && (n == 10 || n == 11);
    }
}
