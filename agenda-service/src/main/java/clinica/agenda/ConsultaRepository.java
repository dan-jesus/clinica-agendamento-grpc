package clinica.agenda;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    List<Consulta> findByDataOrderByHorarioAsc(LocalDate data);

    List<Consulta> findAllByOrderByDataAscHorarioAsc();

    boolean existsByDataAndHorario(LocalDate data, String horario);
}
