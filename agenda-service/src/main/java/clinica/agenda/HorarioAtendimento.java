package clinica.agenda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;

@Entity
@Table(name = "horarios_atendimento")
public class HorarioAtendimento {

    @Id
    private LocalTime horario;

    @Column(nullable = false)
    private boolean ativo;

    protected HorarioAtendimento() {
    }

    public HorarioAtendimento(LocalTime horario, boolean ativo) {
        this.horario = horario;
        this.ativo = ativo;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
