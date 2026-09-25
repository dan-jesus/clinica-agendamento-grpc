package clinica.cadastro;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PacienteRepository extends JpaRepository<PacienteEntity, Long> {

    boolean existsByCpf(String cpf);

    List<PacienteEntity> findAllByOrderByNomeAscIdAsc();
}
