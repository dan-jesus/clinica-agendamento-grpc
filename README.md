# Agendamento de Consultas Odontológicas via gRPC

Trabalho 1 - Sistemas Distribuídos

Comunicação interna de backend entre dois microsserviços usando gRPC e Protocol Buffers, com execução em infraestrutura no Google Cloud.

## Tema

Clínica odontológica. A recepção solicita o agendamento de consultas e o serviço de agenda decide se o horário pode ser confirmado, considerando a grade de atendimento da clínica e os horários já reservados.

## Arquitetura

```text
recepcao-service (Microsserviço A - cliente)
        |
        | gRPC :9090
        v
agenda-service (Microsserviço B - servidor)
        |
        v
Agenda em memória (data -> horários reservados)
```

| Módulo | Responsabilidade |
| --- | --- |
| `contratos-grpc` | Contrato `agenda.proto` e classes geradas pelo Protobuf |
| `agenda-service` | Servidor gRPC: valida a solicitação e mantém a agenda |
| `recepcao-service` | Cliente gRPC: lê os dados no terminal e exibe a resposta |

As classes geradas ficam apenas em `contratos-grpc/target/generated-sources/protobuf/` e não são versionadas.

## Contrato

O arquivo `contratos-grpc/src/main/proto/agenda.proto` define dois procedimentos remotos:

| RPC | Entrada | Saída |
| --- | --- | --- |
| `ConsultarDisponibilidade` | data | data e lista de horários livres |
| `AgendarConsulta` | paciente, procedimento, data e horário | confirmação, protocolo, mensagem e alternativas |

O tipo do procedimento é um `enum` (`LIMPEZA`, `RESTAURACAO`, `CANAL`, `EXTRACAO`, `CLAREAMENTO`), e as listas de horários usam campos `repeated`.

## Regras do serviço de agenda

- Grade de atendimento: 09:00, 10:00, 11:00, 14:00, 15:00, 16:00 e 17:00.
- A data deve estar no formato `dd/mm/aaaa` e existir no calendário.
- O paciente e o procedimento são obrigatórios.
- Um horário já reservado não pode ser reservado novamente. Nesse caso a resposta traz os horários ainda livres da data solicitada.
- A agenda é mantida em memória e é reiniciada junto com o servidor.

## Requisitos

- JDK 21
- Maven Wrapper (incluído no projeto)

## Execução local

Compilar e instalar os módulos, na raiz do projeto:

```bash
./mvnw clean install
```

No Windows:

```powershell
.\mvnw.cmd clean install
```

Iniciar o servidor:

```bash
./mvnw -pl agenda-service exec:java -Dexec.mainClass=clinica.agenda.ServidorAgenda
```

Em outro terminal, iniciar o cliente:

```bash
./mvnw -pl recepcao-service exec:java -Dexec.mainClass=clinica.recepcao.ClienteRecepcao
```

No Windows, os mesmos comandos em uma única linha e com o parâmetro entre aspas:

```powershell
.\mvnw.cmd -pl agenda-service exec:java "-Dexec.mainClass=clinica.agenda.ServidorAgenda"
.\mvnw.cmd -pl recepcao-service exec:java "-Dexec.mainClass=clinica.recepcao.ClienteRecepcao"
```

### Host e porta

Host e porta não são fixos no código. Ambos os serviços aceitam argumentos de linha de comando e usam valores padrão quando nada é informado.

| Serviço | Argumentos | Padrão |
| --- | --- | --- |
| `agenda-service` | porta | `9090` |
| `recepcao-service` | host e porta | `localhost` e `9090` |

Exemplos:

```powershell
.\mvnw.cmd -pl agenda-service exec:java "-Dexec.mainClass=clinica.agenda.ServidorAgenda" "-Dexec.args=8080"
.\mvnw.cmd -pl recepcao-service exec:java "-Dexec.mainClass=clinica.recepcao.ClienteRecepcao" "-Dexec.args=192.168.0.10 8080"
```

## Execução no Google Cloud Platform

O servidor roda em uma VM do Compute Engine e o cliente é executado na máquina local, conectando pelo IP externo da VM.

### Regra de firewall

A porta usada pelo serviço precisa estar liberada para tráfego de entrada.

Regra utilizada:

| Campo | Valor |
| --- | --- |
| Nome | `trabalho-sd-grpc` |
| Direção | Entrada |
| Destino | Tag `trabalho-sd` |
| Intervalos de origem | `0.0.0.0/0` |
| Protocolos e portas | `tcp:8080,9090,50051` |

A VM precisa ter a tag de rede `trabalho-sd` para que a regra se aplique a ela.

A regra libera mais de uma porta para permitir a troca sem alteração de código, caso a porta padrão esteja bloqueada na rede de origem.

### Preparação da VM

```bash
sudo apt update
sudo apt install -y git openjdk-21-jdk

git clone <URL_DO_REPOSITORIO>
cd clinica-agendamento-grpc

./mvnw clean install
```

### Execução

Na VM:

```bash
./mvnw -pl agenda-service exec:java -Dexec.mainClass=clinica.agenda.ServidorAgenda
```

Na máquina local, informando o IP externo da VM:

```powershell
.\mvnw.cmd -pl recepcao-service exec:java "-Dexec.mainClass=clinica.recepcao.ClienteRecepcao" "-Dexec.args=IP_EXTERNO_DA_VM 9090"
```

## Demonstração

1. Consultar a disponibilidade de uma data: todos os horários aparecem livres.
2. Agendar uma consulta em um dos horários: retorna `CONFIRMADO` com protocolo.
3. Consultar a disponibilidade da mesma data: o horário agendado não aparece mais na lista.
4. Agendar outra consulta no mesmo horário: retorna `RECUSADO` com a mensagem `Horário já ocupado.` e a lista de horários ainda livres.

## Discentes - Grupo 05

| Integrante | Matrícula | Responsabilidade |
| --- | ---: | --- |
| Daniel Soares de Jesus | 202107006 | Desenvolvimento |
| Matheus Felipe Araújo de Moraes | 202204398 | Testes e validação |
| Murilo Bernardo | 202203524 | Testes e validação |
