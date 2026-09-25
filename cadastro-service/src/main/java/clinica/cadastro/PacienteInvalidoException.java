package clinica.cadastro;

import java.util.Map;

/**
 * Lançada pelo {@link PacienteValidator} quando os dados de um paciente não
 * passam nas regras de validação (equivalente à MethodArgumentNotValidException
 * do projeto de referência, adaptada para gRPC).
 */
public class PacienteInvalidoException extends RuntimeException {

    private final Map<String, String> erros;

    public PacienteInvalidoException(String mensagem, Map<String, String> erros) {
        super(mensagem);
        this.erros = erros;
    }

    public Map<String, String> getErros() {
        return erros;
    }
}
