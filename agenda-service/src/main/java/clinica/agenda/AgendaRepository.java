package clinica.agenda;

import clinica.grpc.Procedimento;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Camada de acesso a dados da agenda, agora apoiada em Spring Data JPA
 * (ConsultaRepository / HorarioAtendimentoRepository) em vez de JDBC manual.
 * Mantém a mesma API usada por AgendaServiceImpl para minimizar o impacto na
 * camada gRPC.
 */
@Service
public class AgendaRepository {

    private final ConsultaRepository consultaRepository;
    private final HorarioAtendimentoRepository horarioAtendimentoRepository;

    public AgendaRepository(ConsultaRepository consultaRepository,
                             HorarioAtendimentoRepository horarioAtendimentoRepository) {
        this.consultaRepository = consultaRepository;
        this.horarioAtendimentoRepository = horarioAtendimentoRepository;
    }

    public List<LocalTime> horariosAtendimento() {
        return horarioAtendimentoRepository.findByAtivoTrueOrderByHorarioAsc().stream()
                .map(HorarioAtendimento::getHorario)
                .toList();
    }

    public List<LocalTime> horariosOcupados(LocalDate data) {
        return consultaRepository.findByDataOrderByHorarioAsc(data).stream()
                .map(ConsultaEntity::getHorario)
                .toList();
    }

    @Transactional
    public ConsultaRegistro inserir(String protocolo,
                                     Long pacienteId,
                                     String pacienteNome,
                                     Procedimento procedimento,
                                     LocalDate data,
                                     LocalTime horario) {
        if (consultaRepository.existsByDataAndHorario(data, horario)) {
            throw new HorarioIndisponivelException("Horário já ocupado.", List.of());
        }
        try {
            ConsultaEntity salva = consultaRepository.save(
                    new ConsultaEntity(protocolo, pacienteId, pacienteNome, procedimento.name(), data, horario));
            return toRegistro(salva);
        } catch (DataIntegrityViolationException ex) {
            throw new HorarioIndisponivelException("Horário já ocupado.", List.of());
        }
    }

    public List<ConsultaRegistro> listar(Optional<LocalDate> data) {
        List<ConsultaEntity> consultas = data
                .map(consultaRepository::findByDataOrderByHorarioAsc)
                .orElseGet(consultaRepository::findAllByOrderByDataDescHorarioAsc);
        return consultas.stream().map(AgendaRepository::toRegistro).toList();
    }

    private static ConsultaRegistro toRegistro(ConsultaEntity entity) {
        return new ConsultaRegistro(
                entity.getId(),
                entity.getProtocolo(),
                entity.getPacienteId(),
                entity.getPacienteNome(),
                Procedimento.valueOf(entity.getProcedimento()),
                entity.getData(),
                entity.getHorario(),
                entity.getCriadoEm());
    }
}
