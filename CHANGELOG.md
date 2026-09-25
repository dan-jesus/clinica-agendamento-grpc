# Changelog acadêmico

## 2.0.0 — Trabalho 2

- README consolidado dos Trabalhos 1 e 2.
- Diagramas C4 de contexto/containers e sequência em Mermaid.
- Novo `api-gateway` Spring Boot com REST, Bean Validation e Spring Security.
- Autenticação JWT na borda.
- Novo `cadastro-service` gRPC.
- `agenda-service` migrado de memória para PostgreSQL.
- Dois bancos lógicos: `clinica_cadastro` e `clinica_agenda`.
- Grade de horários armazenada e consultada no PostgreSQL.
- Frontend funcional de recepção.
- Operações reais de `SELECT`, `INSERT` e `UPDATE`.
- Docker Compose para execução ponta a ponta.
- Scripts para demonstrar 400, 401, 200/201 e persistência.
- Workflow de CI Maven.
- `recepcao-service` preservado como cliente legado do Trabalho 1.

## 1.0.0 — Trabalho 1

- Contrato `agenda.proto`.
- Cliente gRPC de recepção.
- Servidor gRPC de agenda.
- Agenda em memória.
- Execução demonstrada em Google Cloud Compute Engine.
