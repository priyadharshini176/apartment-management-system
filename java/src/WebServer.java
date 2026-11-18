import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Web version of the system. Uses Java's built-in HTTP server (no extra libraries)
 * and the same ManagementService / database as the console app.
 *
 * Run from the project root, then open http://localhost:8080
 */
public class WebServer {

    private static final ManagementService service = new ManagementService();
    private static final Path WEB_DIR = Paths.get("web").toAbsolutePath().normalize();

    public static void main(String[] args) throws Exception {
        DatabaseConnection.initialiseIfNeeded(true);
        int port = Integer.parseInt(System.getProperty("port", System.getenv().getOrDefault("PORT", "8080")));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", WebServer::handleApi);
        server.createContext("/", WebServer::handleStatic);
        server.start();
        System.out.println("Web app running. Open http://localhost:" + port + "  (press Ctrl+C to stop)");
    }

    // ------------------------------------------------------------------ API

    private static void handleApi(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        Map<String, String> query = parseForm(ex.getRequestURI().getRawQuery());
        try {
            if (method.equals("GET")) {
                switch (path) {
                    case "/api/apartments": send(ex, 200, apartmentsJson()); return;
                    case "/api/residents":  send(ex, 200, residentsJson());  return;
                    case "/api/payments":   send(ex, 200, paymentsJson(blankToNull(query.get("status")))); return;
                    case "/api/complaints": send(ex, 200, complaintsJson(blankToNull(query.get("status")))); return;
                    case "/api/reports":    send(ex, 200, reportsJson()); return;
                    default: send(ex, 404, error("Unknown API path"));   return;
                }
            }
            if (method.equals("POST")) {
                Map<String, String> f = parseForm(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                switch (path) {
                    case "/api/apartments": {
                        int id = service.addApartment(f.get("block"), f.get("aptNo"), f.get("type"), f.get("occupancy"));
                        send(ex, 200, obj("id", id, "message", "Apartment added")); return;
                    }
                    case "/api/residents": {
                        int id = service.addResident(f.get("name"), f.get("contact"), intOf(f, "apartmentId"));
                        send(ex, 200, obj("id", id, "message", "Resident added")); return;
                    }
                    case "/api/charges": {
                        int id = service.addMonthlyCharge(intOf(f, "apartmentId"), f.get("month"), doubleOf(f, "amount"));
                        send(ex, 200, obj("id", id, "message", "Monthly charge recorded (Pending)")); return;
                    }
                    case "/api/payments/pay": {
                        service.markPaid(intOf(f, "paymentId"));
                        send(ex, 200, obj("message", "Payment marked as Paid")); return;
                    }
                    case "/api/complaints": {
                        int id = service.registerComplaint(intOf(f, "residentId"), f.get("category"),
                                f.get("description"), f.get("date"));
                        send(ex, 200, obj("id", id, "message", "Complaint registered (Open)")); return;
                    }
                    case "/api/complaints/status": {
                        service.updateComplaintStatus(intOf(f, "complaintId"), f.get("status"));
                        send(ex, 200, obj("message", "Complaint status updated")); return;
                    }
                    default: send(ex, 404, error("Unknown API path")); return;
                }
            }
            send(ex, 405, error("Method not allowed"));
        } catch (ValidationException e) {
            send(ex, 400, error(e.getMessage()));
        } catch (NumberFormatException e) {
            send(ex, 400, error("Please enter valid numbers."));
        } catch (Exception e) {
            send(ex, 500, error("Server error: " + e.getMessage()));
        }
    }

    // ------------------------------------------------------------------ JSON builders

    private static String apartmentsJson() throws Exception {
        List<String> rows = new ArrayList<>();
        for (Apartment a : service.listApartments())
            rows.add(obj("id", a.getApartmentId(), "block", a.getBlockNo(), "aptNo", a.getApartmentNo(),
                    "type", a.getApartmentType(), "occupancy", a.getOccupancy()));
        return arr(rows);
    }

    private static String residentsJson() throws Exception {
        List<String> rows = new ArrayList<>();
        for (Resident r : service.listResidents())
            rows.add(obj("id", r.getResidentId(), "name", r.getName(), "contact", r.getContact(),
                    "apartmentId", r.getApartmentId(), "aptNo", r.getApartmentNo()));
        return arr(rows);
    }

    private static String paymentsJson(String status) throws Exception {
        List<String> rows = new ArrayList<>();
        for (MaintenancePayment p : service.listPayments(status))
            rows.add(obj("id", p.getPaymentId(), "aptNo", p.getApartmentNo(), "month", p.getMonth(),
                    "amount", p.getAmount(), "status", p.getStatus(), "paidDate", p.getPaidDate()));
        return arr(rows);
    }

    private static String complaintsJson(String status) throws Exception {
        List<String> rows = new ArrayList<>();
        for (Complaint c : service.listComplaints(status))
            rows.add(obj("id", c.getComplaintId(), "resident", c.getResidentName(), "category", c.getCategory(),
                    "description", c.getDescription(), "date", c.getComplaintDate(),
                    "status", c.getStatus(), "resolvedDate", c.getResolvedDate()));
        return arr(rows);
    }

    private static String reportsJson() throws Exception {
        List<MaintenancePayment> payments = service.listPayments(null);
        double charged = payments.stream().mapToDouble(MaintenancePayment::getAmount).sum();
        double pending = payments.stream().filter(p -> p.getStatus().equals("Pending"))
                .mapToDouble(MaintenancePayment::getAmount).sum();
        List<Complaint> complaints = service.listComplaints(null);
        long unresolved = complaints.stream().filter(c -> !c.getStatus().equals("Resolved")).count();

        Map<String, double[]> monthly = new TreeMap<>();   // month -> {collected, pending}
        for (MaintenancePayment p : payments) {
            double[] v = monthly.computeIfAbsent(p.getMonth(), k -> new double[2]);
            v[p.getStatus().equals("Paid") ? 0 : 1] += p.getAmount();
        }
        List<String> monthRows = new ArrayList<>();
        monthly.forEach((m, v) -> monthRows.add(obj("month", m, "collected", v[0], "pending", v[1])));

        return obj("totals", new Raw(obj("charged", charged, "collected", charged - pending, "pending", pending,
                        "complaints", complaints.size(), "unresolved", unresolved)),
                "pendingByApartment", new Raw(mapObj(service.pendingAmountByApartment())),
                "categories", new Raw(mapObj(service.complaintsByCategory())),
                "status", new Raw(mapObj(service.complaintsByStatus())),
                "avgDays", new Raw(mapObj(service.averageResolutionDays())),
                "monthly", new Raw(arr(monthRows)));
    }

    /** Wrapper for text that is already JSON. */
    private static class Raw {
        final String json;
        Raw(String json) { this.json = json; }
    }

    private static String obj(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append(",");
            sb.append(quote((String) kv[i])).append(":").append(value(kv[i + 1]));
        }
        return sb.append("}").toString();
    }

    private static String mapObj(Map<String, ? extends Number> m) {
        return "{" + m.entrySet().stream()
                .map(e -> quote(e.getKey()) + ":" + value(e.getValue()))
                .collect(Collectors.joining(",")) + "}";
    }

    private static String arr(List<String> items) {
        return "[" + String.join(",", items) + "]";
    }

    private static String value(Object v) {
        if (v == null) return "null";
        if (v instanceof Raw) return ((Raw) v).json;
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        return quote(v.toString());
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }

    private static String error(String message) {
        return obj("error", message);
    }

    // ------------------------------------------------------------------ static files

    private static void handleStatic(HttpExchange ex) throws IOException {
        String p = ex.getRequestURI().getPath();
        if (p.equals("/")) p = "/index.html";
        Path file = WEB_DIR.resolve(p.substring(1)).normalize();
        if (!file.startsWith(WEB_DIR) || !Files.isRegularFile(file)) {
            send(ex, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
            return;
        }
        send(ex, 200, contentType(file.toString()), Files.readAllBytes(file));
    }

    private static String contentType(String name) {
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css"))  return "text/css; charset=utf-8";
        if (name.endsWith(".js"))   return "application/javascript; charset=utf-8";
        if (name.endsWith(".png"))  return "image/png";
        return "application/octet-stream";
    }

    // ------------------------------------------------------------------ helpers

    private static void send(HttpExchange ex, int code, String json) throws IOException {
        send(ex, code, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private static Map<String, String> parseForm(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            String k = i < 0 ? pair : pair.substring(0, i);
            String v = i < 0 ? "" : pair.substring(i + 1);
            map.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return map;
    }

    private static int intOf(Map<String, String> f, String key) {
        return Integer.parseInt(f.getOrDefault(key, "").trim());
    }

    private static double doubleOf(Map<String, String> f, String key) {
        return Double.parseDouble(f.getOrDefault(key, "").trim());
    }

    private static String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
}
