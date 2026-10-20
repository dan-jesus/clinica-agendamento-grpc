# Especificação técnica — Trabalho 2

Clínica odontológica: Frontend → API Gateway (REST/JSON + JWT) → microsserviços gRPC → PostgreSQL.

## Componentes

| Módulo | Pacote Java | Porta | Responsabilidade |
|---|---|---|---|
| `frontend/` | — | servido pelo Gateway | HTML/CSS/JS puros; fala só com o Gateway (`/api/v1`) |
| `api-gateway` | `clinica.gateway` | HTTP 8080 | Serve o frontend, autentica (JWT), valida JSON, traduz JSON ↔ Protobuf, chama os serviços gRPC |
| `agenda-service` | `clinica.agenda` | gRPC 9090 | Grade de horários, agendamento e listagem de consultas (tabela `consultas`). Chama `PacienteService.BuscarPaciente` ao agendar |
| `paciente-service` | `clinica.paciente` | gRPC 9091 | Cadastro e consulta de pacientes (tabela `pacientes`) |
| `contratos-grpc` | `clinica.grpc.agenda`, `clinica.grpc.paciente` | — | `agenda.proto` e `paciente.proto` (classes geradas) |

Stack: Java 21, Spring Boot 4.1 (parent no `pom.xml` raiz), Spring gRPC (`spring-boot-starter-grpc-server` / `-client`),
Spring Data JPA + driver PostgreSQL, Spring Security (oauth2 resource server, JWT HS256), Bean Validation.

Um único banco PostgreSQL (`clinica_t2`) para os dois serviços. Cada serviço só acessa a própria tabela; não há
chave estrangeira entre `consultas` e `pacientes` (a ligação é feita via gRPC).

## Variáveis de ambiente

| Variável | Quem usa | Padrão (desenvolvimento) |
|---|---|---|
| `DB_URL` | paciente, agenda | `jdbc:postgresql://localhost:5432/clinica_t2` |
| `DB_USER` | paciente, agenda | `postgres` |
| `DB_PASSWORD` | paciente, agenda | vazio |
| `GRPC_PORT` | paciente (9091), agenda (9090) | ver tabela acima |
| `PACIENTE_GRPC_TARGET` | agenda, gateway | `static://localhost:9091` |
| `AGENDA_GRPC_TARGET` | gateway | `static://localhost:9090` |
| `GRPC_DEADLINE` | gateway | `5s` (agenda → paciente usa 2s) |
| `SERVER_PORT` | gateway | `8080` |
| `APP_USERNAME` / `APP_PASSWORD` | gateway | `admin` / `admin` |
| `JWT_SECRET` | gateway | segredo didático com 32+ bytes |
| `JWT_EXPIRATION_SECONDS` | gateway | `3600` |

Teste local (máquina de desenvolvimento): PostgreSQL temporário em `localhost:5433`, usuário `postgres`, sem senha,
banco `clinica_t2` → `DB_URL=jdbc:postgresql://localhost:5433/clinica_t2`.

## Regras de negócio

**Paciente**
- `nome` obrigatório (2–120 caracteres); `cpf` obrigatório, exatamente 11 dígitos (o Gateway aceita com ou sem
  máscara e envia só dígitos), único; `telefone` obrigatório (10–11 dígitos, mesma normalização); `email` opcional,
  formato válido se informado.

**Agenda**
- Grade fixa: `09:00, 10:00, 11:00, 14:00, 15:00, 16:00, 17:00`, de segunda a sábado (domingo sem horários).
- `data` no formato ISO `yyyy-MM-dd`; não pode ser no passado. Para hoje, só horários ainda não passados.
- Um horário por data: restrição única `(data, horario)` na tabela `consultas` — impede duplicidade mesmo em
  chamadas concorrentes.
- Ao agendar: valida dados → chama `BuscarPaciente` (deadline 2s) → grava a consulta com `paciente_nome`
  (cópia do nome no momento do agendamento, pois os serviços não fazem join) → devolve a consulta com
  `protocolo` no formato `AGD-XXXXXXXX`.

## Códigos de status gRPC (serviços)

| Situação | Status gRPC |
|---|---|
| Campo ausente/inválido, data no passado, horário fora da grade | `INVALID_ARGUMENT` |
| Paciente inexistente | `NOT_FOUND` |
| CPF já cadastrado, horário já ocupado | `ALREADY_EXISTS` |
| Serviço de pacientes fora do ar (visto pela agenda) | `UNAVAILABLE` |
| Prazo excedido | `DEADLINE_EXCEEDED` |

A descrição do `Status` (`withDescription`) leva uma mensagem em português para o usuário final.

## API REST do Gateway (`/api/v1`)

Todas as rotas exigem `Authorization: Bearer <token>`, exceto `POST /api/v1/auth/login` e os arquivos estáticos do
frontend. Corpo de erro sempre: `{"message": "texto em português"}`.

| Método e rota | Corpo / parâmetros | Sucesso | Erros |
|---|---|---|---|
| `POST /auth/login` | `{"usuario","senha"}` | 200 `{"accessToken","tokenType":"Bearer","expiresIn":3600}` | 400 campos ausentes, 401 credencial errada |
| `GET /pacientes` | — | 200 `[Paciente]` | 401 |
| `GET /pacientes/{id}` | — | 200 `Paciente` | 401, 404 |
| `POST /pacientes` | `{"nome","cpf","telefone","email"}` | 201 `Paciente` + header `Location` | 400, 401, 409 CPF duplicado |
| `GET /agenda/disponibilidade?data=yyyy-MM-dd` | `data` obrigatório | 200 `{"data","horariosLivres":["09:00",...]}` | 400, 401 |
| `GET /consultas?data=yyyy-MM-dd` | `data` opcional | 200 `[Consulta]` | 400, 401 |
| `POST /consultas` | `{"pacienteId","procedimento","data","horario"}` | 201 `Consulta` | 400, 401, 404 paciente, 409 horário ocupado |

```json
// Paciente
{"id": 1, "nome": "Ana Souza", "cpf": "12345678901", "telefone": "62999998888", "email": "ana@email.com", "criadoEm": "2026-10-20T14:03:00Z"}
// Consulta
{"id": 7, "protocolo": "AGD-1A2B3C4D", "pacienteId": 1, "pacienteNome": "Ana Souza", "procedimento": "LIMPEZA",
 "data": "2026-10-22", "horario": "10:00", "criadoEm": "2026-10-20T14:05:00Z"}
```

`procedimento` aceita: `LIMPEZA`, `RESTAURACAO`, `CANAL`, `EXTRACAO`, `CLAREAMENTO` (outro valor → 400).

Mapeamento gRPC → HTTP no Gateway (`@RestControllerAdvice`): `INVALID_ARGUMENT` 400, `NOT_FOUND` 404,
`ALREADY_EXISTS` 409, `UNAVAILABLE` 503, `DEADLINE_EXCEEDED` 504, demais 500. Sem token / token inválido ou expirado →
401 barrado no filtro, antes do controller.

## Boas práticas (slides)

- Deadline em toda chamada gRPC.
- Logar cada requisição no Gateway (método, rota, status) e cada chamada nos serviços.
- Não adicionar nada além do pedido: sem Docker, sem H2, sem mensageria, sem outras linguagens.
