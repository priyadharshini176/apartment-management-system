// In-browser demo backend.
// Used automatically when the Java server is not available (for example on GitHub Pages).
// It follows the same rules as ManagementService.java. Data is kept in this browser only.
(function () {
  const KEY = "apartment-demo-v1";
  const TYPES = ["1BHK", "2BHK", "3BHK"];
  const OCC = ["Occupied", "Vacant"];
  const CATS = ["Water", "Electrical", "Cleanliness", "Other"];
  const STATUSES = ["Open", "In Progress", "Resolved"];

  function seed() {
    return {
      apartments: [
        [1, "A", "A-101", "1BHK", "Occupied"], [2, "A", "A-102", "2BHK", "Occupied"],
        [3, "A", "A-201", "2BHK", "Occupied"], [4, "B", "B-101", "3BHK", "Occupied"],
        [5, "B", "B-102", "1BHK", "Vacant"], [6, "B", "B-201", "3BHK", "Occupied"],
      ].map(([id, block, aptNo, type, occupancy]) => ({ id, block, aptNo, type, occupancy })),
      residents: [
        [1, "Arun Kumar", "9876500001", 1], [2, "Meena Devi", "9876500002", 2],
        [3, "Ravi Shankar", "9876500003", 3], [4, "Lakshmi Priya", "9876500004", 4],
        [5, "Karthik Raja", "9876500005", 6],
      ].map(([id, name, contact, apartmentId]) => ({ id, name, contact, apartmentId })),
      payments: [
        [1, 1, "2026-09", 1500, "Paid", "2026-09-05"], [2, 1, "2026-10", 1500, "Paid", "2026-10-06"],
        [3, 1, "2026-11", 1500, "Pending", null], [4, 2, "2026-09", 2000, "Paid", "2026-09-04"],
        [5, 2, "2026-10", 2000, "Pending", null], [6, 2, "2026-11", 2000, "Pending", null],
        [7, 3, "2026-09", 2000, "Paid", "2026-09-10"], [8, 3, "2026-10", 2000, "Paid", "2026-10-08"],
        [9, 3, "2026-11", 2000, "Paid", "2026-11-03"], [10, 4, "2026-09", 2500, "Paid", "2026-09-02"],
        [11, 4, "2026-10", 2500, "Paid", "2026-10-03"], [12, 4, "2026-11", 2500, "Pending", null],
        [13, 6, "2026-09", 2500, "Pending", null], [14, 6, "2026-10", 2500, "Pending", null],
        [15, 6, "2026-11", 2500, "Pending", null],
      ].map(([id, apartmentId, month, amount, status, paidDate]) => ({ id, apartmentId, month, amount, status, paidDate })),
      complaints: [
        [1, 1, "Water", "No water supply in morning", "2026-09-03", "Resolved", "2026-09-05"],
        [2, 2, "Electrical", "Corridor light not working", "2026-09-10", "Resolved", "2026-09-11"],
        [3, 3, "Cleanliness", "Garbage not collected", "2026-09-15", "Resolved", "2026-09-16"],
        [4, 4, "Water", "Low water pressure", "2026-09-22", "Resolved", "2026-09-26"],
        [5, 5, "Water", "Water tank leakage", "2026-10-02", "Resolved", "2026-10-07"],
        [6, 1, "Electrical", "Lift power failure", "2026-10-09", "Resolved", "2026-10-10"],
        [7, 2, "Water", "Dirty water in taps", "2026-10-18", "Resolved", "2026-10-24"],
        [8, 3, "Cleanliness", "Staircase not cleaned", "2026-10-25", "Resolved", "2026-10-27"],
        [9, 5, "Other", "Parking space dispute", "2026-11-01", "In Progress", null],
        [10, 4, "Water", "No water supply", "2026-11-05", "In Progress", null],
        [11, 1, "Electrical", "Street light broken", "2026-11-08", "Open", null],
        [12, 5, "Cleanliness", "Drainage overflow", "2026-11-10", "Open", null],
      ].map(([id, residentId, category, description, date, status, resolvedDate]) =>
        ({ id, residentId, category, description, date, status, resolvedDate })),
    };
  }

  let db = null;
  try { db = JSON.parse(localStorage.getItem(KEY)); } catch (e) { db = null; }
  if (!db || !db.apartments) db = seed();
  function save() { try { localStorage.setItem(KEY, JSON.stringify(db)); } catch (e) { /* storage not available */ } }

  const fail = (msg) => { throw new Error(msg); };
  const nextId = (list) => list.reduce((m, x) => Math.max(m, x.id), 0) + 1;
  const apt = (id) => db.apartments.find((a) => a.id === id);
  const today = () => {
    const d = new Date();
    return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
  };
  const toInt = (v) => { const n = Number(String(v ?? "").trim()); if (!Number.isInteger(n)) fail("Please enter valid numbers."); return n; };
  const toNum = (v) => { const n = Number(String(v ?? "").trim()); if (!isFinite(n) || String(v ?? "").trim() === "") fail("Please enter valid numbers."); return n; };
  const count = (list, key) => list.reduce((m, x) => { m[x[key]] = (m[x[key]] || 0) + 1; return m; }, {});
  const sortKeys = (obj) => Object.fromEntries(Object.entries(obj).sort((a, b) => a[0].localeCompare(b[0])));
  const days = (a, b) => Math.round((Date.parse(b) - Date.parse(a)) / 86400000);

  function get(route, q) {
    const status = q.get("status") || null;
    switch (route) {
      case "apartments":
        return [...db.apartments].sort((a, b) => a.aptNo.localeCompare(b.aptNo));
      case "residents":
        return db.residents.map((r) => ({ id: r.id, name: r.name, contact: r.contact, apartmentId: r.apartmentId, aptNo: apt(r.apartmentId).aptNo }));
      case "payments":
        return db.payments.filter((p) => !status || p.status === status)
          .map((p) => ({ id: p.id, aptNo: apt(p.apartmentId).aptNo, month: p.month, amount: p.amount, status: p.status, paidDate: p.paidDate }))
          .sort((a, b) => a.month.localeCompare(b.month) || a.aptNo.localeCompare(b.aptNo));
      case "complaints":
        return db.complaints.filter((c) => !status || c.status === status)
          .map((c) => ({ id: c.id, resident: db.residents.find((r) => r.id === c.residentId).name, category: c.category,
            description: c.description, date: c.date, status: c.status, resolvedDate: c.resolvedDate }))
          .sort((a, b) => a.date.localeCompare(b.date) || a.id - b.id);
      case "reports": return reports();
      default: return fail("Unknown API path");
    }
  }

  function reports() {
    const charged = db.payments.reduce((s, p) => s + p.amount, 0);
    const pending = db.payments.filter((p) => p.status === "Pending").reduce((s, p) => s + p.amount, 0);
    const pendingBy = {};
    db.payments.filter((p) => p.status === "Pending").forEach((p) => {
      const k = apt(p.apartmentId).aptNo; pendingBy[k] = (pendingBy[k] || 0) + p.amount;
    });
    const resolved = db.complaints.filter((c) => c.status === "Resolved" && c.resolvedDate);
    const sums = {};
    resolved.forEach((c) => { (sums[c.category] = sums[c.category] || []).push(days(c.date, c.resolvedDate)); });
    const avg = Object.fromEntries(Object.entries(sums).map(([k, v]) => [k, v.reduce((a, b) => a + b, 0) / v.length]));
    const months = {};
    db.payments.forEach((p) => {
      const m = (months[p.month] = months[p.month] || { month: p.month, collected: 0, pending: 0 });
      m[p.status === "Paid" ? "collected" : "pending"] += p.amount;
    });
    return {
      totals: { charged, collected: charged - pending, pending, complaints: db.complaints.length,
        unresolved: db.complaints.filter((c) => c.status !== "Resolved").length },
      pendingByApartment: sortKeys(pendingBy),
      categories: sortKeys(count(db.complaints, "category")),
      status: sortKeys(count(db.complaints, "status")),
      avgDays: sortKeys(avg),
      monthly: Object.values(months).sort((a, b) => a.month.localeCompare(b.month)),
    };
  }

  function post(route, f) {
    let out;
    switch (route) {
      case "apartments": {
        const block = (f.block || "").trim().toUpperCase(), aptNo = (f.aptNo || "").trim().toUpperCase();
        if (!block) fail("Block number is required.");
        if (!aptNo) fail("Apartment number is required.");
        if (!TYPES.includes(f.type)) fail("Type must be one of [" + TYPES.join(", ") + "]");
        if (!OCC.includes(f.occupancy)) fail("Occupancy must be one of [" + OCC.join(", ") + "]");
        if (db.apartments.some((a) => a.aptNo === aptNo)) fail("Apartment " + aptNo + " already exists.");
        const id = nextId(db.apartments);
        db.apartments.push({ id, block, aptNo, type: f.type, occupancy: f.occupancy });
        out = { id, message: "Apartment added" }; break;
      }
      case "residents": {
        const name = (f.name || "").trim();
        if (!name) fail("Name is required.");
        if (!/^\d{10}$/.test(f.contact || "")) fail("Contact must be exactly 10 digits.");
        const aid = toInt(f.apartmentId);
        const a = apt(aid);
        if (!a) fail("Apartment ID " + aid + " does not exist.");
        const id = nextId(db.residents);
        db.residents.push({ id, name, contact: f.contact, apartmentId: aid });
        a.occupancy = "Occupied";
        out = { id, message: "Resident added" }; break;
      }
      case "charges": {
        const aid = toInt(f.apartmentId), amount = toNum(f.amount);
        if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(f.month || "")) fail("Month must be in YYYY-MM format, e.g. 2026-12.");
        if (amount <= 0) fail("Amount must be greater than zero.");
        if (!apt(aid)) fail("Apartment ID " + aid + " does not exist.");
        if (db.payments.some((p) => p.apartmentId === aid && p.month === f.month))
          fail("A charge for this apartment and month already exists.");
        const id = nextId(db.payments);
        db.payments.push({ id, apartmentId: aid, month: f.month, amount, status: "Pending", paidDate: null });
        out = { id, message: "Monthly charge recorded (Pending)" }; break;
      }
      case "payments/pay": {
        const id = toInt(f.paymentId);
        const p = db.payments.find((x) => x.id === id);
        if (!p) fail("Payment ID " + id + " does not exist.");
        if (p.status === "Paid") fail("Payment is already marked Paid.");
        p.status = "Paid"; p.paidDate = today();
        out = { message: "Payment marked as Paid" }; break;
      }
      case "complaints": {
        const rid = toInt(f.residentId);
        if (!CATS.includes(f.category)) fail("Category must be one of [" + CATS.join(", ") + "]");
        const desc = (f.description || "").trim();
        if (!desc) fail("Description is required.");
        if (!db.residents.some((r) => r.id === rid)) fail("Resident ID " + rid + " does not exist.");
        const date = (f.date || "").trim() || today();
        if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || isNaN(Date.parse(date))) fail("Date must be in YYYY-MM-DD format.");
        const id = nextId(db.complaints);
        db.complaints.push({ id, residentId: rid, category: f.category, description: desc, date, status: "Open", resolvedDate: null });
        out = { id, message: "Complaint registered (Open)" }; break;
      }
      case "complaints/status": {
        const id = toInt(f.complaintId);
        if (!STATUSES.includes(f.status)) fail("Status must be one of [" + STATUSES.join(", ") + "]");
        const c = db.complaints.find((x) => x.id === id);
        if (!c) fail("Complaint ID " + id + " does not exist.");
        if (c.status === "Resolved" && f.status !== "Resolved") fail("A resolved complaint cannot be reopened.");
        c.status = f.status; c.resolvedDate = f.status === "Resolved" ? today() : null;
        out = { message: "Complaint status updated" }; break;
      }
      default: fail("Unknown API path");
    }
    save();
    return out;
  }

  window.demoApi = function (path, data) {
    const [route, query] = path.split("?");
    return data ? post(route, data) : get(route, new URLSearchParams(query || ""));
  };
  window.demoReset = function () {
    try { localStorage.removeItem(KEY); } catch (e) { /* ignore */ }
    db = seed();
  };
})();
