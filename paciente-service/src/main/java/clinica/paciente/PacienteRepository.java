package clinica.paciente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    boolean existsByCpf(String cpf);

    List<Paciente> findAllByOrderByNomeAsc();
}
