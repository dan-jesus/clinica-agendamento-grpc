package clinica.agenda;

import java.util.Map;

/**
 * Lançada pelo {@link AgendamentoValidator} quando os dados de uma solicitação
 * de agendamento não passam nas regras de validação (equivalente à
 * MethodArgumentNotValidException do projeto de referência, adaptada para gRPC).
 */
public class ConsultaInvalidaException extends RuntimeException {

    private final Map<String, String> erros;

    public ConsultaInvalidaException(String mensagem, Map<String, String> erros) {
        super(mensagem);
        this.erros = erros;
    }

    public Map<String, String> getErros() {
        return erros;
    }
}
