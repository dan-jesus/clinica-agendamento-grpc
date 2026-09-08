package clinica.recepcao;

import clinica.grpc.AgendaServiceGrpc;
import clinica.grpc.AgendarConsultaRequest;
import clinica.grpc.AgendarConsultaResponse;
import clinica.grpc.ConsultarDisponibilidadeRequest;
import clinica.grpc.ConsultarDisponibilidadeResponse;
import clinica.grpc.Procedimento;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.List;
import java.util.Scanner;

public class ClienteRecepcao {

    private static final String HOST_PADRAO = "localhost";
    private static final int PORTA_PADRAO = 9090;

    private static final List<Procedimento> PROCEDIMENTOS = List.of(
            Procedimento.LIMPEZA,
            Procedimento.RESTAURACAO,
            Procedimento.CANAL,
            Procedimento.EXTRACAO,
            Procedimento.CLAREAMENTO);

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : HOST_PADRAO;
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : PORTA_PADRAO;

        ManagedChannel canal = ManagedChannelBuilder.forAddress(host, porta).usePlaintext().build();
        AgendaServiceGrpc.AgendaServiceBlockingStub agenda = AgendaServiceGrpc.newBlockingStub(canal);
        Scanner entrada = new Scanner(System.in);

        System.out.println("Recepção conectada ao serviço de agenda em " + host + ":" + porta);

        boolean ativo = true;
        while (ativo) {
            System.out.println();
            System.out.println("1 - Consultar horários disponíveis");
            System.out.println("2 - Agendar consulta");
            System.out.println("0 - Sair");
            System.out.print("Opção: ");

            String opcao = entrada.nextLine().trim();
            try {
                switch (opcao) {
                    case "1" -> consultarDisponibilidade(agenda, entrada);
                    case "2" -> agendarConsulta(agenda, entrada);
                    case "0" -> ativo = false;
                    default -> System.out.println("Opção inválida.");
                }
            } catch (StatusRuntimeException excecao) {
                System.out.println("Falha na comunicação com o serviço de agenda: " + excecao.getStatus().getCode());
            }
        }

        canal.shutdown();
    }

    private static void consultarDisponibilidade(AgendaServiceGrpc.AgendaServiceBlockingStub agenda,
                                                 Scanner entrada) {
        System.out.print("Data (dd/mm/aaaa): ");
        String data = entrada.nextLine().trim();

        ConsultarDisponibilidadeResponse resposta = agenda.consultarDisponibilidade(
                ConsultarDisponibilidadeRequest.newBuilder().setData(data).build());

        System.out.println(resposta.getHorariosLivresCount() == 0
                ? "Nenhum horário livre em " + resposta.getData() + "."
                : "Horários livres em " + resposta.getData() + ": "
                  + String.join(", ", resposta.getHorariosLivresList()));
    }

    private static void agendarConsulta(AgendaServiceGrpc.AgendaServiceBlockingStub agenda, Scanner entrada) {
        System.out.print("Paciente: ");
        String paciente = entrada.nextLine().trim();

        Procedimento procedimento = lerProcedimento(entrada);

        System.out.print("Data (dd/mm/aaaa): ");
        String data = entrada.nextLine().trim();

        System.out.print("Horário (hh:mm): ");
        String horario = entrada.nextLine().trim();

        AgendarConsultaResponse resposta = agenda.agendarConsulta(
                AgendarConsultaRequest.newBuilder()
                        .setPaciente(paciente)
                        .setProcedimento(procedimento)
                        .setData(data)
                        .setHorario(horario)
                        .build());

        System.out.println();
        System.out.println(resposta.getConfirmado() ? "CONFIRMADO" : "RECUSADO");
        System.out.println(resposta.getMensagem());

        if (resposta.getConfirmado()) {
            System.out.println("Protocolo: " + resposta.getProtocolo());
        } else if (resposta.getHorariosAlternativosCount() > 0) {
            System.out.println("Horários disponíveis: "
                    + String.join(", ", resposta.getHorariosAlternativosList()));
        }
    }

    private static Procedimento lerProcedimento(Scanner entrada) {
        for (int i = 0; i < PROCEDIMENTOS.size(); i++) {
            System.out.println((i + 1) + " - " + PROCEDIMENTOS.get(i).name());
        }
        System.out.print("Procedimento: ");

        int escolha = numeroInformado(entrada.nextLine().trim());
        return escolha >= 1 && escolha <= PROCEDIMENTOS.size()
               ? PROCEDIMENTOS.get(escolha - 1)
               : Procedimento.PROCEDIMENTO_NAO_INFORMADO;
    }

    private static int numeroInformado(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException excecao) {
            return -1;
        }
    }
}
