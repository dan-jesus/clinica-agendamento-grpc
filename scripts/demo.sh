#!/usr/bin/env bash
set -euo pipefail
BASE="http://localhost:8080"

echo "1) 401 sem JWT"
curl -i -s "$BASE/api/pacientes" | head -n 1

echo "2) Login"
LOGIN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"admin@clinica.com","senha":"admin123"}')
TOKEN=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])' <<< "$LOGIN")

echo "3) 400 com payload inválido"
curl -i -s -X POST "$BASE/api/pacientes" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"nome":"Sem CPF","cpf":""}' | head -n 1

echo "Use a interface em http://localhost:5173 para o fluxo visual completo."
