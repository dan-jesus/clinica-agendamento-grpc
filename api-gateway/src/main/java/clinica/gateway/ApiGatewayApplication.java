package clinica.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Ponto único de entrada: REST/JSON + JWT na frente, gRPC atrás. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
