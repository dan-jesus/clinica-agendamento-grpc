# API REST — API Gateway

Base local: `http://localhost:8080`

## Público

### `POST /auth/login`

```json
{
  "email": "admin@clinica.com",
  "senha": "admin123"
}
```

Retorna `200` com JWT.

### `GET /health`

Retorna o status do Gateway.

## Protegido por JWT

Cabeçalho obrigatório:

```text
Authorization: Bearer <token>
```

### `POST /api/pacientes`

```json
{
  "nome": "Ana Souza",
  "cpf": "12345678901",
  "telefone": "62999999999",
  "email": "ana@email.com"
}
```

Retorna `201 Created`.

### `GET /api/pacientes`

Lista os pacientes persistidos.

### `GET /api/pacientes/{id}`

Busca um paciente no `cadastro-service`.

### `PUT /api/pacientes/{id}`

Atualiza o paciente por gRPC e executa `UPDATE` real no PostgreSQL. Usa o mesmo payload de cadastro e retorna `200 OK`.

### `GET /api/agenda/disponibilidade?data=2026-10-23`

Retorna a grade livre consultando o `agenda-service`.

### `POST /api/consultas`

```json
{
  "pacienteId": 1,
  "procedimento": "LIMPEZA",
  "data": "2026-10-23",
  "horario": "14:00"
}
```

Retorna `201 Created` quando confirmado ou `409 Conflict` quando o horário acabou de ser ocupado.

### `GET /api/consultas`

Lista consultas persistidas.

Também aceita `?data=2026-10-23`.
