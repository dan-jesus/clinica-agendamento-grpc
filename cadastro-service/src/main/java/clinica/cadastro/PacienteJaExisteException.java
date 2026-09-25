package clinica.cadastro;

/**
 * Sinaliza violação da constraint UNIQUE(cpf) da tabela pacientes.
 */
public class PacienteJaExisteException extends RuntimeException {

    public PacienteJaExisteException(String mensagem) {
        super(mensagem);
    }

    public PacienteJaExisteException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
