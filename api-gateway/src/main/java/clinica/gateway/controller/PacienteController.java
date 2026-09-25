package clinica.gateway.controller;

import clinica.gateway.dto.PacienteRequest;
import clinica.gateway.dto.PacienteResponse;
import clinica.gateway.exception.BusinessException;
import clinica.gateway.exception.ResourceNotFoundException;
import clinica.gateway.grpc.CadastroGrpcClient;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {
    private final CadastroGrpcClient cadastro;

    public PacienteController(CadastroGrpcClient cadastro) {
        this.cadastro = cadastro;
    }

    @PostMapping
    public ResponseEntity<PacienteResponse> cadastrar(@Valid @RequestBody PacienteRequest request) {
        clinica.grpc.PacienteResponse resposta = cadastro.cadastrar(request);
        if (!resposta.getSucesso()) {
            throw new BusinessException(resposta.getMensagem());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(CadastroGrpcClient.toDto(resposta.getPaciente()));
    }

    @PutMapping("/{id}")
    public PacienteResponse atualizar(@PathVariable("id") long id, @Valid @RequestBody PacienteRequest request) {
        clinica.grpc.PacienteResponse resposta = cadastro.atualizar(id, request);
        if (!resposta.getSucesso()) {
            if (resposta.getMensagem().contains("não encontrado")) {
                throw new ResourceNotFoundException(resposta.getMensagem());
            }
            throw new BusinessException(resposta.getMensagem());
        }
        return CadastroGrpcClient.toDto(resposta.getPaciente());
    }

    @GetMapping
    public List<PacienteResponse> listar() {
        return cadastro.listar();
    }

    @GetMapping("/{id}")
    public PacienteResponse buscar(@PathVariable("id") long id) {
        clinica.grpc.PacienteResponse resposta = cadastro.buscar(id);
        if (!resposta.getSucesso()) {
            throw new ResourceNotFoundException(resposta.getMensagem());
        }
        return CadastroGrpcClient.toDto(resposta.getPaciente());
    }
}
