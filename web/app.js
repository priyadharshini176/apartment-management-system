// Front end for the Apartment Manager. Talks to the Java server through /api/*.
const $ = (sel) => document.querySelector(sel);
const rs = (n) => "Rs. " + Number(n).toLocaleString("en-IN", { maximumFractionDigits: 0 });

// Safe text: everything from the server is escaped before it goes into HTML.
function esc(v) {
  return String(v ?? "").replace(/[&<>"']/g,
    (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
const badge = (s) => `<span class="badge b-${esc(s).replace(/ /g, "-")}">${esc(s)}</span>`;

let toastTimer;
function toast(msg, isError) {
  const t = $("#toast");
  t.textContent = msg;
  t.className = "show" + (isError ? " err" : "");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (t.className = ""), 3200);
}

// Talks to the Java server. If there is none (for example on GitHub Pages),
// it switches to the in-browser demo backend in demo.js.
let demoMode = false;
async function api(path, data) {
  if (!demoMode) {
    try {
      const opts = data ? { method: "POST", body: new URLSearchParams(data) } : {};
      const res = await fetch("api/" + path, opts);
      if ((res.headers.get("content-type") || "").includes("application/json")) {
        const json = await res.json();
        if (!res.ok) throw new Error(json.error || "Request failed");
        return json;
      }
      demoMode = true;                     // no Java backend answered with JSON
    } catch (e) {
      if (!(e instanceof TypeError)) throw e;
      demoMode = true;                     // network failure
    }
    if (demoMode) $("#demo-banner").hidden = false;
  }
  return window.demoApi(path, data);
}

$("#demo-reset").addEventListener("click", () => {
  window.demoReset();
  toast("Demo data reset");
  const active = document.querySelector("#tabs button.active");
  refresh(active ? active.dataset.tab : "dashboard");
});

// ---------------------------------------------------------------- tabs
document.querySelectorAll("#tabs button").forEach((b) =>
  b.addEventListener("click", () => {
    document.querySelectorAll("#tabs button").forEach((x) => x.classList.toggle("active", x === b));
    document.querySelectorAll(".tab").forEach((t) => t.classList.toggle("active", t.id === b.dataset.tab));
    refresh(b.dataset.tab);
  }));

function table(el, headers, rows, emptyText) {
  if (!rows.length) { el.innerHTML = `<tr><td class="empty">${emptyText}</td></tr>`; return; }
  el.innerHTML = "<thead><tr>" + headers.map((h) => `<th>${h}</th>`).join("") + "</tr></thead><tbody>" +
    rows.map((r) => "<tr>" + r.map((c) => `<td${c.cls ? ` class="${c.cls}"` : ""}>${c.html ?? esc(c)}</td>`).join("") + "</tr>").join("") +
    "</tbody>";
}

// ---------------------------------------------------------------- lists
async function loadApartments() {
  const list = await api("apartments");
  table($("#t-apartments"), ["ID", "Block", "Apartment", "Type", "Occupancy"],
    list.map((a) => [a.id, a.block, a.aptNo, a.type, { html: badge(a.occupancy) }]), "No apartments yet.");
  const opts = list.map((a) => `<option value="${a.id}">${esc(a.aptNo)} (${esc(a.type)})</option>`).join("");
  document.querySelectorAll(".apt-select").forEach((s) => (s.innerHTML = opts));
}

async function loadResidents() {
  const list = await api("residents");
  table($("#t-residents"), ["ID", "Name", "Contact", "Apartment"],
    list.map((r) => [r.id, r.name, r.contact, r.aptNo]), "No residents yet.");
  $("#res-select").innerHTML = list.map((r) =>
    `<option value="${r.id}">${esc(r.name)} (${esc(r.aptNo)})</option>`).join("");
}

async function loadPayments() {
  const list = await api("payments?status=" + encodeURIComponent($("#pay-filter").value));
  table($("#t-payments"), ["ID", "Apartment", "Month", "Amount", "Status", "Paid on", ""],
    list.map((p) => [p.id, p.aptNo, p.month, rs(p.amount), { html: badge(p.status) }, p.paidDate || "-",
      { html: p.status === "Pending" ? `<button class="small-btn" data-pay="${p.id}">Mark paid</button>` : "" }]),
    "No payments found.");
}

async function loadComplaints() {
  const list = await api("complaints?status=" + encodeURIComponent($("#cmp-filter").value));
  const statuses = ["Open", "In Progress", "Resolved"];
  table($("#t-complaints"), ["ID", "Resident", "Category", "Description", "Date", "Status", "Update"],
    list.map((c) => [c.id, c.resident, c.category, { cls: "desc", html: esc(c.description) }, c.date,
      { html: badge(c.status) },
      { html: c.status === "Resolved" ? "-" :
        `<select data-cmp="${c.id}">${statuses.map((s) =>
          `<option${s === c.status ? " selected" : ""}>${s}</option>`).join("")}</select>` }]),
    "No complaints found.");
}

// ---------------------------------------------------------------- dashboard
const charts = {};
const COLORS = { blue: "#2f5fd0", green: "#2e9e5b", red: "#d64545", amber: "#e08a1e", purple: "#7a5cc4", grey: "#9aa5b4" };

function draw(id, config) {
  if (charts[id]) charts[id].destroy();
  config.options = Object.assign({ responsive: true, maintainAspectRatio: false }, config.options || {});
  charts[id] = new Chart($("#" + id), config);
}

async function loadDashboard() {
  const r = await api("reports");
  $("#k-charged").textContent = rs(r.totals.charged);
  $("#k-collected").textContent = rs(r.totals.collected);
  $("#k-pending").textContent = rs(r.totals.pending);
  $("#k-complaints").textContent = r.totals.complaints;
  $("#k-unresolved").textContent = r.totals.unresolved;

  const cats = ["Water", "Electrical", "Cleanliness", "Other"];
  draw("c-category", { type: "bar",
    data: { labels: cats, datasets: [{ data: cats.map((c) => r.categories[c] || 0), backgroundColor: COLORS.blue, borderRadius: 6 }] },
    options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } } });

  const sts = ["Open", "In Progress", "Resolved"];
  draw("c-status", { type: "doughnut",
    data: { labels: sts, datasets: [{ data: sts.map((s) => r.status[s] || 0), backgroundColor: [COLORS.red, COLORS.amber, COLORS.green] }] },
    options: { plugins: { legend: { position: "bottom" } } } });

  draw("c-monthly", { type: "bar",
    data: { labels: r.monthly.map((m) => m.month), datasets: [
      { label: "Collected", data: r.monthly.map((m) => m.collected), backgroundColor: COLORS.green },
      { label: "Pending", data: r.monthly.map((m) => m.pending), backgroundColor: COLORS.red }] },
    options: { plugins: { legend: { position: "bottom" } }, scales: { x: { stacked: true }, y: { stacked: true, beginAtZero: true } } } });

  const pend = Object.entries(r.pendingByApartment).sort((a, b) => b[1] - a[1]);
  draw("c-pending", { type: "bar",
    data: { labels: pend.map((p) => p[0]), datasets: [{ data: pend.map((p) => p[1]), backgroundColor: COLORS.red, borderRadius: 6 }] },
    options: { indexAxis: "y", plugins: { legend: { display: false } }, scales: { x: { beginAtZero: true } } } });

  const days = Object.entries(r.avgDays);
  draw("c-days", { type: "bar",
    data: { labels: days.map((d) => d[0]), datasets: [{ data: days.map((d) => Number(d[1].toFixed(1))), backgroundColor: COLORS.purple, borderRadius: 6 }] },
    options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } } });
}

function refresh(tab) {
  const jobs = { dashboard: loadDashboard, apartments: loadApartments, residents: loadResidents,
                 payments: loadPayments, complaints: loadComplaints };
  const run = async () => {
    try {
      // dropdowns need apartments / residents before a form is used
      if (tab === "residents" || tab === "payments") await loadApartments();
      if (tab === "complaints") await loadResidents();
      await jobs[tab]();
    } catch (e) { toast(e.message, true); }
  };
  run();
}

// ---------------------------------------------------------------- forms
function wireForm(id, path, after) {
  $(id).addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.target;
    try {
      const res = await api(path, Object.fromEntries(new FormData(form)));
      toast(res.message);
      form.reset();
      await after();
    } catch (err) { toast(err.message, true); }
  });
}
wireForm("#f-apartment", "apartments", loadApartments);
wireForm("#f-resident", "residents", async () => { await loadApartments(); await loadResidents(); });
wireForm("#f-charge", "charges", loadPayments);
wireForm("#f-complaint", "complaints", loadComplaints);

$("#pay-filter").addEventListener("change", loadPayments);
$("#cmp-filter").addEventListener("change", loadComplaints);

$("#t-payments").addEventListener("click", async (e) => {
  const id = e.target.dataset.pay;
  if (!id) return;
  try { toast((await api("payments/pay", { paymentId: id })).message); await loadPayments(); }
  catch (err) { toast(err.message, true); }
});

$("#t-complaints").addEventListener("change", async (e) => {
  const id = e.target.dataset.cmp;
  if (!id) return;
  try { toast((await api("complaints/status", { complaintId: id, status: e.target.value })).message); }
  catch (err) { toast(err.message, true); }
  await loadComplaints();
});

refresh("dashboard");
