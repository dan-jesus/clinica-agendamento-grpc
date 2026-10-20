'use strict';

const API = '/api/v1';
const $ = (id) => document.getElementById(id);

const PROCEDIMENTOS = {
  LIMPEZA: 'Limpeza', RESTAURACAO: 'Restauração', CANAL: 'Canal',
  EXTRACAO: 'Extração', CLAREAMENTO: 'Clareamento'
};

let horarioEscolhido = null;
let alertaTimer = null;

/* ---------- Sessão ---------- */
const getToken = () => sessionStorage.getItem('accessToken');
const setToken = (t) => sessionStorage.setItem('accessToken', t);
const limparToken = () => sessionStorage.removeItem('accessToken');

/* ---------- Requisições (única função de fetch) ---------- */
async function api(metodo, caminho, corpo) {
  const headers = { 'Accept': 'application/json' };
  if (corpo !== undefined) headers['Content-Type'] = 'application/json';
  if (getToken()) headers['Authorization'] = 'Bearer ' + getToken();

  let resp;
  try {
    resp = await fetch(API + caminho, {
      method: metodo, headers, body: corpo !== undefined ? JSON.stringify(corpo) : undefined
    });
  } catch (e) {
    mostrarStatus(0, 'Sem conexão');
    throw new Error('Não foi possível conectar ao servidor.');
  }
  mostrarStatus(resp.status, resp.statusText);

  let dados = null;
  const texto = await resp.text();
  if (texto) { try { dados = JSON.parse(texto); } catch (e) { dados = null; } }

  if (!resp.ok) {
    if (resp.status === 401 && caminho !== '/auth/login') {
      limparToken();
      mostrarTela('login');
      throw new Error('Sessão expirada. Faça login novamente.');
    }
    throw new Error((dados && dados.message) || ('Erro ' + resp.status));
  }
  return dados;
}

/* ---------- Mensagens ---------- */
function mostrarStatus(codigo, texto) {
  const b = $('http-badge');
  const padrao = { 200: 'OK', 201: 'Created', 400: 'Bad Request', 401: 'Unauthorized', 404: 'Not Found', 409: 'Conflict', 503: 'Service Unavailable', 504: 'Gateway Timeout' };
  b.hidden = false;
  b.textContent = codigo ? `HTTP ${codigo} ${texto || padrao[codigo] || ''}`.trim() : texto;
  b.classList.toggle('erro', codigo === 0 || codigo >= 400);
}

function alerta(msg, tipo) {
  const el = $('alerta');
  el.textContent = msg;
  el.className = 'alerta ' + tipo;
  el.hidden = false;
  clearTimeout(alertaTimer);
  alertaTimer = setTimeout(() => { el.hidden = true; }, 7000);
}
const erro = (e) => alerta(e.message, 'erro');
const sucesso = (m) => alerta(m, 'ok');

/* ---------- Navegação entre telas ---------- */
function mostrarTela(nome) {
  document.querySelectorAll('.view').forEach((v) => { v.hidden = v.id !== 'view-' + nome; });
  $('nav').hidden = nome === 'login';
  document.querySelectorAll('.nav-btn').forEach((b) => b.classList.toggle('ativo', b.dataset.view === nome));
  $('alerta').hidden = true;
  if (nome === 'pacientes') carregarPacientes().catch(erro);
  if (nome === 'agendar') prepararAgendar();
  if (nome === 'consultas') carregarConsultas().catch(erro);
}

/* ---------- Utilidades ---------- */
const soDigitos = (s) => s.replace(/\D/g, '');

function mascaraCpf(v) {
  const d = soDigitos(v).slice(0, 11);
  return d.replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d{1,2})$/, '$1-$2');
}
function mascaraTelefone(v) {
  const d = soDigitos(v).slice(0, 11);
  if (d.length <= 2) return d ? '(' + d : '';
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  const corte = d.length > 10 ? 7 : 6;
  return `(${d.slice(0, 2)}) ${d.slice(2, corte)}-${d.slice(corte)}`;
}
function formatarData(iso) {
  const [a, m, d] = String(iso).split('-');
  return `${d}/${m}/${a}`;
}
function celula(tr, texto) {
  const td = document.createElement('td');
  td.textContent = texto;
  tr.appendChild(td);
}
function linhaVazia(tbody, colunas, msg) {
  tbody.innerHTML = '';
  const tr = document.createElement('tr');
  const td = document.createElement('td');
  td.colSpan = colunas; td.className = 'vazio'; td.textContent = msg;
  tr.appendChild(td); tbody.appendChild(tr);
}

/* ---------- Login ---------- */
async function entrar(ev) {
  ev.preventDefault();
  const usuario = $('login-usuario').value.trim();
  const senha = $('login-senha').value;
  if (!usuario || !senha) return alerta('Informe usuário e senha.', 'erro');
  try {
    const r = await api('POST', '/auth/login', { usuario, senha });
    setToken(r.accessToken);
    $('login-senha').value = '';
    mostrarTela('pacientes');
  } catch (e) { erro(e); }
}
function sair() {
  limparToken();
  mostrarTela('login');
}

/* ---------- Pacientes ---------- */
async function carregarPacientes() {
  const lista = await api('GET', '/pacientes');
  const tbody = $('lista-pacientes');
  if (!lista.length) return linhaVazia(tbody, 4, 'Nenhum paciente cadastrado.');
  tbody.innerHTML = '';
  lista.forEach((p) => {
    const tr = document.createElement('tr');
    celula(tr, p.nome); celula(tr, mascaraCpf(p.cpf || '')); celula(tr, mascaraTelefone(p.telefone || '')); celula(tr, p.email || '—');
    tbody.appendChild(tr);
  });
}

async function cadastrarPaciente(ev) {
  ev.preventDefault();
  const corpo = {
    nome: $('pac-nome').value.trim(),
    cpf: soDigitos($('pac-cpf').value),
    telefone: soDigitos($('pac-tel').value),
    email: $('pac-email').value.trim()
  };
  if (!corpo.nome || !corpo.cpf || !corpo.telefone) return alerta('Preencha nome, CPF e telefone.', 'erro');
  try {
    const p = await api('POST', '/pacientes', corpo);
    sucesso(`Paciente ${p.nome} cadastrado com sucesso.`);
    ev.target.reset();
    await carregarPacientes();
  } catch (e) { erro(e); }
}

/* ---------- Agendar ---------- */
async function prepararAgendar() {
  $('resultado').hidden = true;
  const sel = $('ag-paciente');
  try {
    const lista = await api('GET', '/pacientes');
    sel.innerHTML = '';
    if (!lista.length) sel.innerHTML = '<option value="">Cadastre um paciente primeiro</option>';
    lista.forEach((p) => {
      const o = document.createElement('option');
      o.value = p.id; o.textContent = `${p.nome} (${mascaraCpf(p.cpf || '')})`;
      sel.appendChild(o);
    });
  } catch (e) { erro(e); }
  if ($('ag-data').value) carregarHorarios();
}

async function carregarHorarios() {
  const data = $('ag-data').value;
  const box = $('horarios');
  horarioEscolhido = null;
  if (!data) { box.innerHTML = '<span class="muted">Escolha uma data para ver os horários.</span>'; return; }
  try {
    const r = await api('GET', '/agenda/disponibilidade?data=' + encodeURIComponent(data));
    box.innerHTML = '';
    if (!r.horariosLivres.length) { box.innerHTML = '<span class="muted">Nenhum horário livre nesta data.</span>'; return; }
    r.horariosLivres.forEach((h) => {
      const b = document.createElement('button');
      b.type = 'button'; b.className = 'chip'; b.textContent = h;
      b.setAttribute('role', 'radio'); b.setAttribute('aria-checked', 'false');
      b.addEventListener('click', () => escolherHorario(h));
      box.appendChild(b);
    });
  } catch (e) { box.innerHTML = ''; erro(e); }
}

function escolherHorario(h) {
  horarioEscolhido = h;
  document.querySelectorAll('#horarios .chip').forEach((c) => c.setAttribute('aria-checked', String(c.textContent === h)));
}

async function agendar(ev) {
  ev.preventDefault();
  const pacienteId = Number($('ag-paciente').value);
  const data = $('ag-data').value;
  if (!pacienteId) return alerta('Selecione um paciente.', 'erro');
  if (!data) return alerta('Escolha a data.', 'erro');
  if (!horarioEscolhido) return alerta('Escolha um horário livre.', 'erro');
  try {
    const c = await api('POST', '/consultas', {
      pacienteId, procedimento: $('ag-proc').value, data, horario: horarioEscolhido
    });
    const r = $('resultado');
    r.textContent = `Consulta agendada para ${formatarData(c.data)} às ${c.horario}. Protocolo: ${c.protocolo}`;
    r.hidden = false;
    sucesso('Consulta agendada com sucesso.');
    await carregarHorarios();
  } catch (e) { erro(e); await carregarHorarios().catch(() => {}); }
}

/* ---------- Consultas ---------- */
async function carregarConsultas() {
  const data = $('filtro-data').value;
  const lista = await api('GET', '/consultas' + (data ? '?data=' + encodeURIComponent(data) : ''));
  const tbody = $('lista-consultas');
  if (!lista.length) return linhaVazia(tbody, 5, 'Nenhuma consulta encontrada.');
  tbody.innerHTML = '';
  lista.forEach((c) => {
    const tr = document.createElement('tr');
    celula(tr, formatarData(c.data)); celula(tr, c.horario); celula(tr, c.pacienteNome);
    celula(tr, PROCEDIMENTOS[c.procedimento] || c.procedimento); celula(tr, c.protocolo);
    tbody.appendChild(tr);
  });
}

/* ---------- Inicialização ---------- */
function init() {
  $('form-login').addEventListener('submit', entrar);
  $('btn-sair').addEventListener('click', sair);
  document.querySelectorAll('.nav-btn').forEach((b) => b.addEventListener('click', () => mostrarTela(b.dataset.view)));

  $('form-paciente').addEventListener('submit', cadastrarPaciente);
  $('pac-cpf').addEventListener('input', (e) => { e.target.value = mascaraCpf(e.target.value); });
  $('pac-tel').addEventListener('input', (e) => { e.target.value = mascaraTelefone(e.target.value); });

  $('form-agendar').addEventListener('submit', agendar);
  $('ag-data').addEventListener('change', carregarHorarios);

  $('filtro-data').addEventListener('change', () => carregarConsultas().catch(erro));
  $('btn-limpar').addEventListener('click', () => { $('filtro-data').value = ''; carregarConsultas().catch(erro); });

  mostrarTela(getToken() ? 'pacientes' : 'login');
}
document.addEventListener('DOMContentLoaded', init);
