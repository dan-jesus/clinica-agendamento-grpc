# Roteiro de apresentação — Trabalho 2

Objetivo: demonstrar tudo em até 10 minutos sem depender de improvisação.

## Antes de começar

1. Execute `docker compose up --build -d`.
2. Abra `http://localhost:5173`.
3. Deixe um terminal aberto para `docker compose logs -f api-gateway agenda-service cadastro-service`.
4. Deixe outro terminal pronto para consultar PostgreSQL com os comandos do README.

## 0:00–1:30 — Arquitetura

Abra o README no diagrama C4 de containers e explique:

- Frontend só fala HTTP/JSON com o Gateway.
- Gateway valida JWT e payload.
- Comunicação interna é gRPC/Protobuf.
- Cadastro e Agenda são microsserviços separados.
- Cada domínio persiste em banco PostgreSQL real.

## 1:30–3:00 — Segurança: 401 e login

1. Clique em **Testar 401**: mostrar `401 Unauthorized` sem token.
2. Faça login com `admin@clinica.com` / `admin123`.
3. Explique que o Gateway chama o cadastro-service e, só depois, emite o JWT.

## 3:00–4:00 — Validação: 400

Execute `scripts/demo.ps1` ou faça uma chamada no cliente REST com CPF vazio. Mostre `400 Bad Request` e os campos validados no Gateway.

## 4:00–6:00 — Cadastro real

1. Cadastre um paciente pela interface.
2. Mostre `201 Created` no DevTools/terminal, se desejado.
3. Execute:

```bash
docker exec -it clinica-postgres psql -U postgres -d clinica_cadastro -c "SELECT id,nome,cpf,email FROM pacientes ORDER BY id DESC LIMIT 5;"
```

O novo registro deve aparecer.

## 6:00–8:00 — Agendamento ponta a ponta

1. Selecione o paciente.
2. Escolha procedimento e data.
3. Consulte os horários livres.
4. Selecione um horário e agende.
5. A tabela de consultas é atualizada.

Explique a orquestração: Gateway → BuscarPaciente/cadastro-service → AgendarConsulta/agenda-service.

## 8:00–9:00 — Prova de persistência

```bash
docker exec -it clinica-postgres psql -U postgres -d clinica_agenda -c "SELECT id,protocolo,paciente_nome,procedimento,data,horario FROM consultas ORDER BY id DESC LIMIT 5;"
```

Mostre o registro criado pelo frontend.

## 9:00–10:00 — Código

Mostre rapidamente:

- `contratos-grpc/src/main/proto/*.proto`
- `api-gateway/.../JwtAuthenticationFilter.java`
- `api-gateway/.../ConsultaController.java`
- `agenda-service/.../AgendaRepository.java`
- `cadastro-service/.../CadastroRepository.java`

Encerre antes dos 10 minutos.
