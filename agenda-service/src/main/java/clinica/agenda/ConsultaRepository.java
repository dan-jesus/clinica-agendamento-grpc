package clinica.agenda;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ConsultaRepository extends JpaRepository<ConsultaEntity, Long> {

    List<ConsultaEntity> findByDataOrderByHorarioAsc(LocalDate data);

    List<ConsultaEntity> findAllByOrderByDataDescHorarioAsc();

    boolean existsByDataAndHorario(LocalDate data, LocalTime horario);
}
