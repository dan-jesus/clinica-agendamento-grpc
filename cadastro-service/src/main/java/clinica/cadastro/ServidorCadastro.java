package clinica.cadastro;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ServidorCadastro {
    private static final int PORTA_PADRAO = 9091;

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext contexto = SpringApplication.run(ServidorCadastro.class, args);

        int porta = args.length > 0
                ? Integer.parseInt(args[0])
                : Integer.parseInt(env("CADASTRO_GRPC_PORT", String.valueOf(PORTA_PADRAO)));

        CadastroRepository repository = contexto.getBean(CadastroRepository.class);
        repository.garantirUsuarioInicial(
                env("APP_ADMIN_NAME", "Recepção Clínica"),
                env("APP_ADMIN_EMAIL", "admin@clinica.com"),
                env("APP_ADMIN_PASSWORD", "admin123"));

        Server servidor = ServerBuilder.forPort(porta)
                .addService(new CadastroServiceImpl(repository))
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            servidor.shutdown();
            contexto.close();
        }));

        System.out.println("cadastro-service ativo na porta gRPC " + porta + " com persistência PostgreSQL (JPA/Hibernate).");
        servidor.awaitTermination();
    }

    private static String env(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}
