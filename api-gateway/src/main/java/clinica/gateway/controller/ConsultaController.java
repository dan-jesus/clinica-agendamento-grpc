package clinica.gateway.controller;

import clinica.gateway.dto.ConsultaItemResponse;
import clinica.gateway.dto.ConsultaRequest;
import clinica.gateway.dto.ConsultaResponse;
import clinica.gateway.exception.BusinessException;
import clinica.gateway.grpc.AgendaGrpcClient;
import clinica.gateway.grpc.CadastroGrpcClient;
import clinica.grpc.PacienteResponse;
import clinica.grpc.Procedimento;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/consultas")
public class ConsultaController {
    private final AgendaGrpcClient agenda;
    private final CadastroGrpcClient cadastro;

    public ConsultaController(AgendaGrpcClient agenda, CadastroGrpcClient cadastro) {
        this.agenda = agenda;
        this.cadastro = cadastro;
    }

    @PostMapping
    public ResponseEntity<ConsultaResponse> agendar(@Valid @RequestBody ConsultaRequest request) {
        validarProcedimento(request.procedimento());

        PacienteResponse paciente = cadastro.buscar(request.pacienteId());
        if (!paciente.getSucesso()) {
            throw new BusinessException("Paciente informado não existe.");
        }

        ConsultaResponse resposta = agenda.agendar(request, paciente.getPaciente().getId(), paciente.getPaciente().getNome());
        if (!resposta.confirmado()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(resposta);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping
    public List<ConsultaItemResponse> listar(
            @RequestParam(value = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return agenda.listar(data);
    }

    private static void validarProcedimento(String valor) {
        try {
            Procedimento procedimento = Procedimento.valueOf(valor.trim().toUpperCase());
            if (procedimento == Procedimento.PROCEDIMENTO_NAO_INFORMADO || procedimento == Procedimento.UNRECOGNIZED) {
                throw new IllegalArgumentException();
            }
        } catch (Exception ex) {
            throw new BusinessException("Procedimento inválido. Use LIMPEZA, RESTAURACAO, CANAL, EXTRACAO ou CLAREAMENTO.");
        }
    }
}
