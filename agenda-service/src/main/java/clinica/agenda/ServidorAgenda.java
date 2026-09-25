package clinica.agenda;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ServidorAgenda {
    private static final int PORTA_PADRAO = 9090;

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext contexto = SpringApplication.run(ServidorAgenda.class, args);

        int porta = args.length > 0
                ? Integer.parseInt(args[0])
                : Integer.parseInt(env("AGENDA_GRPC_PORT", String.valueOf(PORTA_PADRAO)));

        AgendaRepository repository = contexto.getBean(AgendaRepository.class);

        Server servidor = ServerBuilder.forPort(porta)
                .addService(new AgendaServiceImpl(repository))
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            servidor.shutdown();
            contexto.close();
        }));

        System.out.println("agenda-service ativo na porta gRPC " + porta + " com persistência PostgreSQL (JPA/Hibernate).");
        servidor.awaitTermination();
    }

    private static String env(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}
