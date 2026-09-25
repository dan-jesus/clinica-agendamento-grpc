const API = `${window.location.protocol}//${window.location.hostname}:8080`;
const state = { token: localStorage.getItem("clinica.jwt"), selectedTime: null };

const $ = (id) => document.getElementById(id);
const loginView = $("loginView");
const appView = $("appView");
const sessionBox = $("sessionBox");

function showToast(message, error = false) {
  const toast = $("toast");
  toast.textContent = message;
  toast.className = `toast${error ? " error" : ""}`;
  setTimeout(() => toast.classList.add("hidden"), 4200);
}

async function api(path, options = {}, useToken = true) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (useToken && state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(`${API}${path}`, { ...options, headers });
  const text = await response.text();
  const body = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const message = body?.mensagem || body?.message || `HTTP ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    error.body = body;
    throw error;
  }
  return { status: response.status, body };
}

function setAuthenticated(authenticated, userName = "") {
  loginView.classList.toggle("hidden", authenticated);
  appView.classList.toggle("hidden", !authenticated);
  sessionBox.classList.toggle("hidden", !authenticated);
  $("userName").textContent = userName || "Sessão autenticada";
}

$("loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const status = $("loginStatus");
  status.classList.add("hidden");
  try {
    const result = await api("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email: $("loginEmail").value, senha: $("loginPassword").value })
    }, false);
    state.token = result.body.token;
    localStorage.setItem("clinica.jwt", state.token);
    setAuthenticated(true, result.body.usuario.nome);
    await Promise.all([loadPatients(), loadAppointments()]);
    showToast("Login realizado. JWT recebido do API Gateway.");
  } catch (error) {
    status.textContent = `${error.status || "Erro"}: ${error.message}`;
    status.classList.remove("hidden");
  }
});

$("logoutBtn").addEventListener("click", () => {
  state.token = null;
  localStorage.removeItem("clinica.jwt");
  setAuthenticated(false);
});

$("patientForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const payload = {
      nome: $("patientName").value.trim(),
      cpf: $("patientCpf").value.replace(/\D/g, ""),
      telefone: $("patientPhone").value.trim(),
      email: $("patientEmail").value.trim()
    };
    const result = await api("/api/pacientes", { method: "POST", body: JSON.stringify(payload) });
    event.target.reset();
    await loadPatients(result.body.id);
    showToast(`201 Created — paciente #${result.body.id} persistido.`);
  } catch (error) {
    showToast(`${error.status || "Erro"}: ${error.message}`, true);
  }
});

async function loadPatients(selectedId = null) {
  try {
    const result = await api("/api/pacientes");
    const select = $("appointmentPatient");
    select.innerHTML = '<option value="">Selecione...</option>';
    for (const patient of result.body) {
      const option = document.createElement("option");
      option.value = patient.id;
      option.textContent = `${patient.nome} — CPF ${patient.cpf}`;
      if (String(patient.id) === String(selectedId)) option.selected = true;
      select.appendChild(option);
    }
  } catch (error) {
    if (error.status === 401) setAuthenticated(false);
  }
}

$("availabilityBtn").addEventListener("click", loadAvailability);
$("appointmentDate").addEventListener("change", () => {
  state.selectedTime = null;
  $("appointmentTime").value = "";
  $("timeSlots").innerHTML = '<span class="muted">Clique em Consultar.</span>';
});

async function loadAvailability() {
  const date = $("appointmentDate").value;
  if (!date) return showToast("Informe uma data antes de consultar.", true);
  try {
    const result = await api(`/api/agenda/disponibilidade?data=${encodeURIComponent(date)}`);
    renderSlots(result.body.horariosLivres);
  } catch (error) {
    showToast(`${error.status || "Erro"}: ${error.message}`, true);
  }
}

function renderSlots(slots) {
  const host = $("timeSlots");
  host.innerHTML = "";
  state.selectedTime = null;
  $("appointmentTime").value = "";
  if (!slots.length) {
    host.innerHTML = '<span class="muted">Nenhum horário livre.</span>';
    return;
  }
  for (const slot of slots) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "time-slot";
    button.textContent = slot;
    button.addEventListener("click", () => {
      document.querySelectorAll(".time-slot").forEach((item) => item.classList.remove("selected"));
      button.classList.add("selected");
      state.selectedTime = slot;
      $("appointmentTime").value = slot;
    });
    host.appendChild(button);
  }
}

$("appointmentForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!state.selectedTime) return showToast("Selecione um horário disponível.", true);
  const payload = {
    pacienteId: Number($("appointmentPatient").value),
    procedimento: $("appointmentProcedure").value,
    data: $("appointmentDate").value,
    horario: state.selectedTime
  };
  try {
    const result = await api("/api/consultas", { method: "POST", body: JSON.stringify(payload) });
    showToast(`${result.status} Created — protocolo ${result.body.protocolo}`);
    await Promise.all([loadAvailability(), loadAppointments()]);
  } catch (error) {
    showToast(`${error.status || "Erro"}: ${error.message}`, true);
    if (error.status === 409) await loadAvailability();
  }
});

async function loadAppointments() {
  try {
    const result = await api("/api/consultas");
    const body = $("appointmentsBody");
    body.innerHTML = "";
    if (!result.body.length) {
      body.innerHTML = '<tr><td colspan="5" class="muted">Nenhuma consulta persistida.</td></tr>';
      return;
    }
    for (const item of result.body) {
      const row = document.createElement("tr");
      row.innerHTML = `<td>${escapeHtml(item.protocolo)}</td><td>${escapeHtml(item.paciente)}</td><td>${escapeHtml(item.procedimento)}</td><td>${escapeHtml(item.data)}</td><td>${escapeHtml(item.horario)}</td>`;
      body.appendChild(row);
    }
  } catch (error) {
    showToast(`${error.status || "Erro"}: ${error.message}`, true);
  }
}

$("refreshAppointmentsBtn").addEventListener("click", loadAppointments);

$("test401Btn").addEventListener("click", async () => {
  try {
    await api("/api/pacientes", {}, false);
    showToast("A chamada sem token foi aceita inesperadamente.", true);
  } catch (error) {
    showToast(`${error.status} Unauthorized — ${error.message}`, error.status !== 401);
  }
});

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>'"]/g, (char) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;"
  })[char]);
}

if (state.token) {
  setAuthenticated(true);
  Promise.all([loadPatients(), loadAppointments()]).catch(() => {});
} else {
  setAuthenticated(false);
}

const today = new Date();
today.setDate(today.getDate() + 1);
$("appointmentDate").value = today.toISOString().slice(0, 10);
