package clinica.agenda;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;

public interface HorarioAtendimentoRepository extends JpaRepository<HorarioAtendimento, LocalTime> {

    List<HorarioAtendimento> findByAtivoTrueOrderByHorarioAsc();
}
