package clinica.agenda;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class ServidorAgenda {

    private static final int PORTA_PADRAO = 9090;

    public static void main(String[] args) throws IOException, InterruptedException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : PORTA_PADRAO;

        Server servidor = ServerBuilder.forPort(porta)
                .addService(new AgendaServiceImpl())
                .build()
                .start();

        System.out.println("Serviço de agenda ouvindo na porta " + porta);
        servidor.awaitTermination();
    }
}
