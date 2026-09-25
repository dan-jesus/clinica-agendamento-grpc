package clinica.gateway.controller;

import clinica.gateway.dto.DisponibilidadeResponse;
import clinica.gateway.grpc.AgendaGrpcClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/agenda")
public class AgendaController {
    private final AgendaGrpcClient agenda;

    public AgendaController(AgendaGrpcClient agenda) {
        this.agenda = agenda;
    }

    @GetMapping("/disponibilidade")
    public DisponibilidadeResponse disponibilidade(
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return agenda.disponibilidade(data);
    }
}
