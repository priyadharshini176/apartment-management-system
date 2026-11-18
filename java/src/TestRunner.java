import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Simple automated test runner (no external libraries).
 * Uses a separate temporary database so real data is never touched.
 * Run from the project root:  see README.md  ->  "Run the tests".
 */
public class TestRunner {

    private static int passed = 0, failed = 0;
    private static final ManagementService s = new ManagementService();

    public static void main(String[] args) throws Exception {
        File db = new File(System.getProperty("db.path", "test_apartment.db"));
        if (db.exists()) db.delete();
        DatabaseConnection.initialiseIfNeeded(true);

        // ---- Residents and apartments
        check("TC01 Add valid apartment", () -> s.addApartment("C", "C-101", "2BHK", "Vacant") > 0);
        expectError("TC02 Duplicate apartment number rejected", () -> s.addApartment("C", "C-101", "2BHK", "Vacant"));
        expectError("TC03 Invalid apartment type rejected", () -> s.addApartment("C", "C-102", "5BHK", "Vacant"));
        check("TC04 Add valid resident", () -> s.addResident("Test User", "9000000001", 7) > 0);
        expectError("TC05 Contact with letters rejected", () -> s.addResident("Bad Phone", "98abc", 7));
        expectError("TC06 Resident for missing apartment rejected", () -> s.addResident("Ghost", "9000000002", 999));
        check("TC07 Resident makes vacant apartment Occupied", () -> {
            for (Apartment a : s.listApartments())
                if (a.getApartmentId() == 7) return a.getOccupancy().equals("Occupied");
            return false;
        });

        // ---- Payments
        check("TC08 Add monthly charge (status Pending)", () -> {
            int id = s.addMonthlyCharge(7, "2026-12", 2000);
            return s.listPayments("Pending").stream().anyMatch(p -> p.getPaymentId() == id);
        });
        expectError("TC09 Duplicate month charge rejected", () -> s.addMonthlyCharge(7, "2026-12", 2000));
        expectError("TC10 Invalid month format rejected", () -> s.addMonthlyCharge(7, "12-2026", 2000));
        expectError("TC11 Zero amount rejected", () -> s.addMonthlyCharge(7, "2027-01", 0));
        check("TC12 Mark payment Paid sets paid date", () -> {
            s.markPaid(3);
            return s.listPayments("Paid").stream()
                    .anyMatch(p -> p.getPaymentId() == 3 && p.getPaidDate() != null);
        });
        expectError("TC13 Paying an already-paid payment rejected", () -> s.markPaid(1));
        check("TC14 Pending total for A-102 is 4000 after sample data", () -> {
            Map<String, Double> m = s.pendingAmountByApartment();
            return m.get("A-102") == 4000.0;
        });

        // ---- Complaints
        check("TC15 Register complaint starts as Open", () -> {
            int id = s.registerComplaint(1, "Water", "Test leak", "2026-11-12");
            return s.listComplaints("Open").stream().anyMatch(c -> c.getComplaintId() == id);
        });
        expectError("TC16 Invalid category rejected", () -> s.registerComplaint(1, "Noise", "x", "2026-11-12"));
        expectError("TC17 Empty description rejected", () -> s.registerComplaint(1, "Water", "  ", "2026-11-12"));
        expectError("TC18 Bad date format rejected", () -> s.registerComplaint(1, "Water", "x", "12/11/2026"));
        check("TC19 Open -> In Progress", () -> {
            s.updateComplaintStatus(11, "In Progress");
            return s.listComplaints("In Progress").stream().anyMatch(c -> c.getComplaintId() == 11);
        });
        check("TC20 In Progress -> Resolved sets resolved date", () -> {
            s.updateComplaintStatus(11, "Resolved");
            return s.listComplaints("Resolved").stream()
                    .anyMatch(c -> c.getComplaintId() == 11 && c.getResolvedDate() != null);
        });
        expectError("TC21 Resolved complaint cannot be reopened", () -> s.updateComplaintStatus(11, "Open"));
        expectError("TC22 Unknown complaint ID rejected", () -> s.updateComplaintStatus(999, "Resolved"));

        // ---- Reports
        check("TC23 Category report: Water count is 5 + 1 new = 6", () -> {
            Map<String, Long> m = s.complaintsByCategory();
            return m.get("Water") == 6L;
        });
        check("TC24 Average resolution days for Electrical (sample data) is positive", () -> {
            Double d = s.averageResolutionDays().get("Electrical");
            return d != null && d > 0;
        });

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        db.delete();
        System.exit(failed == 0 ? 0 : 1);
    }

    interface Check { boolean run() throws Exception; }
    interface Action { void run() throws Exception; }

    private static void check(String name, Check c) {
        try {
            if (c.run()) { passed++; System.out.println("PASS  " + name); }
            else         { failed++; System.out.println("FAIL  " + name); }
        } catch (Exception e) {
            failed++;
            System.out.println("FAIL  " + name + "  (exception: " + e.getMessage() + ")");
        }
    }

    /** The action must throw a ValidationException to pass. */
    private static void expectError(String name, Action a) {
        try {
            a.run();
            failed++;
            System.out.println("FAIL  " + name + "  (no error was raised)");
        } catch (ValidationException e) {
            passed++;
            System.out.println("PASS  " + name + "  -> \"" + e.getMessage() + "\"");
        } catch (Exception e) {
            failed++;
            System.out.println("FAIL  " + name + "  (wrong exception: " + e.getMessage() + ")");
        }
    }
}
