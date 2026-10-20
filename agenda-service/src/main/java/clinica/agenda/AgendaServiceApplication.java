package clinica.agenda;

import clinica.grpc.paciente.PacienteServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GrpcChannelFactory;

@SpringBootApplication
public class AgendaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgendaServiceApplication.class, args);
    }

    // Stub bloqueante para o PacienteService; o endereço vem de spring.grpc.client.channel.paciente.target.
    @Bean
    PacienteServiceGrpc.PacienteServiceBlockingStub pacienteService(GrpcChannelFactory channels) {
        return PacienteServiceGrpc.newBlockingStub(channels.createChannel("paciente"));
    }
}
