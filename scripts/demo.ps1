$ErrorActionPreference = "Stop"
$Base = "http://localhost:8080"

Write-Host "`n1) 401 - rota protegida sem JWT" -ForegroundColor Cyan
try {
    Invoke-RestMethod -Uri "$Base/api/pacientes" -Method Get
} catch {
    Write-Host "HTTP $([int]$_.Exception.Response.StatusCode) - esperado: 401"
}

Write-Host "`n2) Login e obtenção do JWT" -ForegroundColor Cyan
$login = Invoke-RestMethod -Uri "$Base/auth/login" -Method Post -ContentType "application/json" -Body (@{
    email = "admin@clinica.com"
    senha = "admin123"
} | ConvertTo-Json)
$Headers = @{ Authorization = "Bearer $($login.token)" }
Write-Host "JWT recebido para $($login.usuario.nome)"

Write-Host "`n3) 400 - payload inválido" -ForegroundColor Cyan
try {
    Invoke-RestMethod -Uri "$Base/api/pacientes" -Method Post -Headers $Headers -ContentType "application/json" -Body (@{
        nome = "Paciente sem CPF"
        cpf = ""
    } | ConvertTo-Json)
} catch {
    Write-Host "HTTP $([int]$_.Exception.Response.StatusCode) - esperado: 400"
}

$cpf = "9" + (Get-Date -Format "MMddHHmmss")
$cpf = $cpf.Substring(0, [Math]::Min(11, $cpf.Length)).PadRight(11, "0")
Write-Host "`n4) 201 - cadastrar paciente real no PostgreSQL" -ForegroundColor Cyan
$paciente = Invoke-RestMethod -Uri "$Base/api/pacientes" -Method Post -Headers $Headers -ContentType "application/json" -Body (@{
    nome = "Paciente Demonstração"
    cpf = $cpf
    telefone = "62999999999"
    email = "demo@clinica.com"
} | ConvertTo-Json)
Write-Host "Paciente criado: id=$($paciente.id), cpf=$cpf"

$data = (Get-Date).AddDays(7).ToString("yyyy-MM-dd")
Write-Host "`n5) 200 - alterar paciente e persistir UPDATE" -ForegroundColor Cyan
$pacienteAtualizado = Invoke-RestMethod -Uri "$Base/api/pacientes/$($paciente.id)" -Method Put -Headers $Headers -ContentType "application/json" -Body (@{
    nome = "Paciente Demonstração Atualizado"
    cpf = $cpf
    telefone = "62988888888"
    email = "demo.atualizado@clinica.com"
} | ConvertTo-Json)
Write-Host "Paciente atualizado no banco: $($pacienteAtualizado.nome)"

Write-Host "`n6) Consultar disponibilidade via Gateway -> gRPC" -ForegroundColor Cyan
$disp = Invoke-RestMethod -Uri "$Base/api/agenda/disponibilidade?data=$data" -Method Get -Headers $Headers
$horario = $disp.horariosLivres[0]
Write-Host "Primeiro horário livre: $horario"

Write-Host "`n7) 201 - agendar e persistir consulta" -ForegroundColor Cyan
$consulta = Invoke-RestMethod -Uri "$Base/api/consultas" -Method Post -Headers $Headers -ContentType "application/json" -Body (@{
    pacienteId = $paciente.id
    procedimento = "LIMPEZA"
    data = $data
    horario = $horario
} | ConvertTo-Json)
Write-Host "Consulta criada: protocolo=$($consulta.protocolo)"

Write-Host "`n8) Listagem vinda do PostgreSQL" -ForegroundColor Cyan
Invoke-RestMethod -Uri "$Base/api/consultas" -Method Get -Headers $Headers | Format-Table

Write-Host "`nFluxo concluído." -ForegroundColor Green
