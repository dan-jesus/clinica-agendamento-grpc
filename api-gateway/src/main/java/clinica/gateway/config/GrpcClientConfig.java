package clinica.gateway.config;

import clinica.grpc.agenda.AgendaServiceGrpc;
import clinica.grpc.paciente.PacienteServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/** Stubs blocking dos dois microsserviços (alvos em spring.grpc.client.channel.*). */
@Configuration
public class GrpcClientConfig {

    @Bean
    PacienteServiceGrpc.PacienteServiceBlockingStub pacienteStub(GrpcChannelFactory channels) {
        return PacienteServiceGrpc.newBlockingStub(channels.createChannel("pacientes"));
    }

    @Bean
    AgendaServiceGrpc.AgendaServiceBlockingStub agendaStub(GrpcChannelFactory channels) {
        return AgendaServiceGrpc.newBlockingStub(channels.createChannel("agenda"));
    }
}
