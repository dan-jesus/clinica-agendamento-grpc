package clinica.agenda;

import java.util.List;

/**
 * Sinaliza uma regra de negócio de agenda: horário fora do expediente ou já
 * ocupado por outra consulta (inclusive quando detectado pela constraint
 * UNIQUE(data, horario) do banco).
 */
public class HorarioIndisponivelException extends RuntimeException {

    private final List<String> horariosAlternativos;

    public HorarioIndisponivelException(String mensagem, List<String> horariosAlternativos) {
        super(mensagem);
        this.horariosAlternativos = horariosAlternativos;
    }

    public List<String> getHorariosAlternativos() {
        return horariosAlternativos;
    }
}
