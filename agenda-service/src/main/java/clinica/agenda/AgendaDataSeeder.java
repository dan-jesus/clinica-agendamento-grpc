package clinica.agenda;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

/**
 * Garante, na subida do serviço, que a tabela horarios_atendimento (criada
 * automaticamente pelo Hibernate via ddl-auto=update) tenha os horários
 * padrão da clínica. Substitui o INSERT ... ON CONFLICT que antes rodava
 * junto ao DDL manual em AgendaRepository.
 */
@Component
public class AgendaDataSeeder implements CommandLineRunner {

    private static final List<LocalTime> HORARIOS_PADRAO = List.of(
            LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0),
            LocalTime.of(14, 0), LocalTime.of(15, 0), LocalTime.of(16, 0), LocalTime.of(17, 0));

    private final HorarioAtendimentoRepository horarioAtendimentoRepository;

    public AgendaDataSeeder(HorarioAtendimentoRepository horarioAtendimentoRepository) {
        this.horarioAtendimentoRepository = horarioAtendimentoRepository;
    }

    @Override
    public void run(String... args) {
        if (horarioAtendimentoRepository.count() > 0) {
            return;
        }
        HORARIOS_PADRAO.forEach(horario -> horarioAtendimentoRepository.save(new HorarioAtendimento(horario, true)));
    }
}
