package clinica.agenda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "consultas",
        uniqueConstraints = @UniqueConstraint(name = "uq_consulta_data_horario", columnNames = {"data", "horario"}))
public class ConsultaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String protocolo;

    @Column(name = "paciente_id")
    private Long pacienteId;

    @Column(name = "paciente_nome", nullable = false, length = 150)
    private String pacienteNome;

    @Column(nullable = false, length = 40)
    private String procedimento;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime horario;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected ConsultaEntity() {
    }

    public ConsultaEntity(String protocolo, Long pacienteId, String pacienteNome, String procedimento,
                           LocalDate data, LocalTime horario) {
        this.protocolo = protocolo;
        this.pacienteId = pacienteId;
        this.pacienteNome = pacienteNome;
        this.procedimento = procedimento;
        this.data = data;
        this.horario = horario;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public String getPacienteNome() {
        return pacienteNome;
    }

    public String getProcedimento() {
        return procedimento;
    }

    public LocalDate getData() {
        return data;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
