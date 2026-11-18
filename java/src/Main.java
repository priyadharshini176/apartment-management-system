import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** Console menu for the Apartment Maintenance and Resident Complaint Management System. */
public class Main {

    private static final ManagementService service = new ManagementService();
    private static Scanner in;

    public static void main(String[] args) {
        in = new Scanner(System.in);
        try {
            DatabaseConnection.initialiseIfNeeded(true);
        } catch (Exception e) {
            System.out.println("Could not set up the database: " + e.getMessage());
            System.out.println("Run the program from the project root folder (where the sql/ folder is).");
            return;
        }

        System.out.println("=== Apartment Maintenance & Resident Complaint Management System ===");
        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Enter choice: ");
            if (!in.hasNextLine()) break;        // end of input
            String choice = in.nextLine();
            try {
                switch (choice.trim()) {
                    case "1":  addApartment();          break;
                    case "2":  addResident();           break;
                    case "3":  listApartments();        break;
                    case "4":  listResidents();         break;
                    case "5":  addMonthlyCharge();      break;
                    case "6":  markPaid();              break;
                    case "7":  listPayments();          break;
                    case "8":  registerComplaint();     break;
                    case "9":  updateComplaint();       break;
                    case "10": listComplaints();        break;
                    case "11": reports();               break;
                    case "0":  running = false;         break;
                    default:   System.out.println("Invalid choice. Enter a number from the menu.");
                }
            } catch (ValidationException e) {
                System.out.println("Input error: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Input error: please enter a valid number.");
            } catch (Exception e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("---------------- MENU ----------------");
        System.out.println(" 1. Add apartment            2. Add resident");
        System.out.println(" 3. View apartments          4. View residents");
        System.out.println(" 5. Add monthly charge       6. Record payment (mark Paid)");
        System.out.println(" 7. View payments");
        System.out.println(" 8. Register complaint       9. Update complaint status");
        System.out.println("10. View complaints         11. Reports");
        System.out.println(" 0. Exit");
        System.out.println("--------------------------------------");
    }

    // ---------------------------------------------------------------- actions

    private static void addApartment() throws Exception {
        String block = readLine("Block number (e.g. A): ");
        String aptNo = readLine("Apartment number (e.g. A-301): ");
        String type = readLine("Type (1BHK/2BHK/3BHK): ");
        String occ = readLine("Occupancy (Occupied/Vacant): ");
        int id = service.addApartment(block, aptNo, type, occ);
        System.out.println("Apartment added with ID " + id);
    }

    private static void addResident() throws Exception {
        String name = readLine("Resident name: ");
        String contact = readLine("Contact number (10 digits): ");
        int aptId = Integer.parseInt(readLine("Apartment ID: ").trim());
        int id = service.addResident(name, contact, aptId);
        System.out.println("Resident added with ID " + id);
    }

    private static void listApartments() throws Exception {
        System.out.println("ID   Block  Apt No   Type   Occupancy");
        for (Apartment a : service.listApartments()) System.out.println(a);
    }

    private static void listResidents() throws Exception {
        System.out.println("ID   Name                 Contact      Apt No");
        for (Resident r : service.listResidents()) System.out.println(r);
    }

    private static void addMonthlyCharge() throws Exception {
        int aptId = Integer.parseInt(readLine("Apartment ID: ").trim());
        String month = readLine("Month (YYYY-MM): ").trim();
        double amount = Double.parseDouble(readLine("Amount: ").trim());
        int id = service.addMonthlyCharge(aptId, month, amount);
        System.out.println("Charge recorded with payment ID " + id + " (status: Pending)");
    }

    private static void markPaid() throws Exception {
        int id = Integer.parseInt(readLine("Payment ID: ").trim());
        service.markPaid(id);
        System.out.println("Payment " + id + " marked as Paid.");
    }

    private static void listPayments() throws Exception {
        String f = readLine("Filter (Paid/Pending, or press Enter for all): ").trim();
        List<MaintenancePayment> list = service.listPayments(f.isEmpty() ? null : f);
        System.out.println("ID   Apt No   Month    Amount    Status   Paid Date");
        list.forEach(System.out::println);
        System.out.println(list.size() + " record(s).");
    }

    private static void registerComplaint() throws Exception {
        int resId = Integer.parseInt(readLine("Resident ID: ").trim());
        String cat = readLine("Category " + ManagementService.getCategories() + ": ").trim();
        String desc = readLine("Description: ");
        String date = readLine("Date (YYYY-MM-DD, Enter for today): ");
        int id = service.registerComplaint(resId, cat, desc, date);
        System.out.println("Complaint registered with ID " + id + " (status: Open)");
    }

    private static void updateComplaint() throws Exception {
        int id = Integer.parseInt(readLine("Complaint ID: ").trim());
        String status = readLine("New status " + ManagementService.getStatuses() + ": ").trim();
        service.updateComplaintStatus(id, status);
        System.out.println("Complaint " + id + " updated to " + status + ".");
    }

    private static void listComplaints() throws Exception {
        String f = readLine("Filter (Open/In Progress/Resolved, or Enter for all): ").trim();
        List<Complaint> list = service.listComplaints(f.isEmpty() ? null : f);
        System.out.println("ID   Resident         Category     Date       Status       Description");
        list.forEach(System.out::println);
        System.out.println(list.size() + " record(s).");
    }

    private static void reports() throws Exception {
        System.out.println("\n--- Pending maintenance payments (amount per apartment) ---");
        Map<String, Double> pending = service.pendingAmountByApartment();
        pending.forEach((apt, amt) -> System.out.printf("%-8s Rs. %.2f%n", apt, amt));
        double total = pending.values().stream().mapToDouble(Double::doubleValue).sum();
        System.out.printf("TOTAL PENDING: Rs. %.2f%n", total);

        System.out.println("\n--- Category-wise complaints ---");
        service.complaintsByCategory().forEach((k, v) -> System.out.printf("%-12s %d%n", k, v));

        System.out.println("\n--- Complaint status summary ---");
        service.complaintsByStatus().forEach((k, v) -> System.out.printf("%-12s %d%n", k, v));

        System.out.println("\n--- Average resolution time (days) ---");
        service.averageResolutionDays().forEach((k, v) -> System.out.printf("%-12s %.1f%n", k, v));
    }

    // ---------------------------------------------------------------- input helper

    /** Prints the prompt and returns the typed line (empty text if the input has ended). */
    private static String readLine(String prompt) {
        System.out.print(prompt);
        return in.hasNextLine() ? in.nextLine() : "";
    }
}
