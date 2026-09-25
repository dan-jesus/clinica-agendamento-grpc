package clinica.cadastro;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Classe de validação dos dados de um paciente, equivalente em papel à
 * validação por anotações (@NotBlank/@Pattern) do projeto de referência,
 * porém aplicada manualmente porque este serviço expõe uma API gRPC (sem o
 * disparo automático de validação do Spring MVC).
 */
public final class PacienteValidator {

    private static final Pattern SOMENTE_DIGITOS = Pattern.compile("\\d{11}");

    private PacienteValidator() {
    }

    /**
     * @throws PacienteInvalidoException se nome ou CPF forem inválidos, contendo em
     *                                   {@link PacienteInvalidoException#getErros()} um erro por campo.
     */
    public static void validar(String nome, String cpf) {
        Map<String, String> erros = new HashMap<>();

        if (nome == null || nome.isBlank()) {
            erros.put("nome", "não deve estar vazio");
        }

        if (cpf == null || cpf.isBlank()) {
            erros.put("cpf", "não deve estar vazio");
        } else if (!SOMENTE_DIGITOS.matcher(cpf.replaceAll("[^0-9]", "")).matches()) {
            erros.put("cpf", "deve conter 11 dígitos");
        }

        if (!erros.isEmpty()) {
            throw new PacienteInvalidoException("Dados do paciente inválidos.", erros);
        }
    }
}
