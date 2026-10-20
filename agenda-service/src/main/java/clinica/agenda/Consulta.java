package clinica.agenda;

import clinica.grpc.agenda.Procedimento;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "consultas",
       uniqueConstraints = @UniqueConstraint(name = "uk_consultas_data_horario", columnNames = {"data", "horario"}))
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String protocolo;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    // Cópia do nome no momento do agendamento (não há join entre serviços).
    @Column(name = "paciente_nome", nullable = false, length = 120)
    private String pacienteNome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Procedimento procedimento;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, length = 5)
    private String horario;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Consulta() {
    }

    public Consulta(String protocolo, Long pacienteId, String pacienteNome, Procedimento procedimento,
                    LocalDate data, String horario) {
        this.protocolo = protocolo;
        this.pacienteId = pacienteId;
        this.pacienteNome = pacienteNome;
        this.procedimento = procedimento;
        this.data = data;
        this.horario = horario;
        this.criadoEm = Instant.now();
    }

    // Converte a entidade para a mensagem Protobuf.
    public clinica.grpc.agenda.Consulta toGrpc() {
        return clinica.grpc.agenda.Consulta.newBuilder()
                .setId(id)
                .setProtocolo(protocolo)
                .setPacienteId(pacienteId)
                .setPacienteNome(pacienteNome)
                .setProcedimento(procedimento)
                .setData(data.toString())
                .setHorario(horario)
                .setCriadoEm(criadoEm.toString())
                .build();
    }

    public String getHorario() {
        return horario;
    }
}
