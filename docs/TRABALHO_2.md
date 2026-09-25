# Trabalho 2 — Frontend, API Gateway, JWT, gRPC e PostgreSQL

Apresentação: **22/10/2026 às 18:50 — Laboratório 154/INF**.

## Arquitetura implementada

- Frontend estático funcional (`frontend`), executado via Nginx no Docker Compose.
- API Gateway com Spring Boot (`api-gateway`).
- Autenticação JWT validada na borda pelo Spring Security.
- `cadastro-service` gRPC para usuários e pacientes.
- `agenda-service` gRPC para disponibilidade e consultas.
- Dois bancos lógicos PostgreSQL: `clinica_cadastro` e `clinica_agenda`.
- Contratos Protocol Buffers centralizados em `contratos-grpc`.

## Matriz de requisitos

| Requisito | Implementação |
| --- | --- |
| Frontend funcional | SPA em HTML/CSS/JS no módulo `frontend` |
| Frontend só chama Gateway | `app.js` usa somente `http://<host>:8080` |
| API Gateway REST/HTTP | Spring Boot + Spring Web |
| Validação do JSON | Jakarta Bean Validation nos DTOs |
| JWT | Spring Security + filtro `JwtAuthenticationFilter` |
| 401 sem token | `JsonAuthenticationEntryPoint` e filtro JWT |
| Mínimo de 2 microsserviços | `cadastro-service` e `agenda-service` |
| Comunicação interna gRPC | clientes gRPC dentro do Gateway |
| Protocol Buffers | `agenda.proto` e `cadastro.proto` |
| Banco real | PostgreSQL 17 no `docker-compose.yml` |
| Sem mocks em memória | JDBC/HikariCP executando SQL real |
| Alteração real | `PUT /api/pacientes/{id}` → gRPC → `UPDATE pacientes` |
| 400 | payload/parâmetro inválido ou regra de negócio |
| 200 | consultas/listagens/login bem-sucedidos |
| 201 | criação de paciente e agendamento |
| Persistência comprovável | comandos `psql` documentados no README |

## Fluxo de agendamento

1. Frontend envia JSON + `Authorization: Bearer <jwt>` ao Gateway.
2. Spring Security valida o JWT.
3. Bean Validation valida o payload.
4. Gateway chama `cadastro-service.BuscarPaciente` via gRPC.
5. `cadastro-service` consulta `clinica_cadastro.pacientes` no PostgreSQL.
6. Se o paciente existe, o Gateway chama `agenda-service.AgendarConsulta` via gRPC.
7. `agenda-service` insere a consulta em `clinica_agenda.consultas`.
8. Gateway converte a resposta Protobuf para JSON e retorna `201 Created`.

## Status HTTP para demonstração

- `401 Unauthorized`: `GET /api/pacientes` sem token.
- `400 Bad Request`: `POST /api/pacientes` com CPF ausente/inválido.
- `200 OK`: login, disponibilidade e listagens.
- `201 Created`: novo paciente e nova consulta.
- `409 Conflict`: tentativa de reservar um horário já ocupado.

## Persistência

Não há `List`, `Map`, mock de repository ou resposta estática como fonte de dados de negócio. As listas exibidas pelo frontend são respostas de consultas SQL realizadas pelos microsserviços no PostgreSQL.
