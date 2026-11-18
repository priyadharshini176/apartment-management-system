import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Business logic and database operations for the whole system. */
public class ManagementService {

    private static final List<String> APARTMENT_TYPES = Arrays.asList("1BHK", "2BHK", "3BHK");
    private static final List<String> OCCUPANCY = Arrays.asList("Occupied", "Vacant");
    private static final List<String> CATEGORIES = Arrays.asList("Water", "Electrical", "Cleanliness", "Other");
    private static final List<String> STATUSES = Arrays.asList("Open", "In Progress", "Resolved");

    public static List<String> getCategories() { return CATEGORIES; }
    public static List<String> getStatuses()   { return STATUSES; }

    // ------------------------------------------------------------------ apartments

    public int addApartment(String block, String aptNo, String type, String occupancy)
            throws SQLException, ValidationException {
        if (block == null || block.trim().isEmpty()) throw new ValidationException("Block number is required.");
        if (aptNo == null || aptNo.trim().isEmpty()) throw new ValidationException("Apartment number is required.");
        if (!APARTMENT_TYPES.contains(type)) throw new ValidationException("Type must be one of " + APARTMENT_TYPES);
        if (!OCCUPANCY.contains(occupancy)) throw new ValidationException("Occupancy must be one of " + OCCUPANCY);

        String sql = "INSERT INTO apartment (block_no, apartment_no, apartment_type, occupancy) VALUES (?,?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, block.trim().toUpperCase());
            ps.setString(2, aptNo.trim().toUpperCase());
            ps.setString(3, type);
            ps.setString(4, occupancy);
            ps.executeUpdate();
            return generatedId(ps);
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                throw new ValidationException("Apartment " + aptNo.trim().toUpperCase() + " already exists.");
            }
            throw e;
        }
    }

    public List<Apartment> listApartments() throws SQLException {
        List<Apartment> list = new ArrayList<>();
        String sql = "SELECT apartment_id, block_no, apartment_no, apartment_type, occupancy "
                   + "FROM apartment ORDER BY apartment_no";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Apartment(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5)));
            }
        }
        return list;
    }

    // ------------------------------------------------------------------ residents

    public int addResident(String name, String contact, int apartmentId)
            throws SQLException, ValidationException {
        if (name == null || name.trim().isEmpty()) throw new ValidationException("Name is required.");
        if (contact == null || !contact.matches("\\d{10}"))
            throw new ValidationException("Contact must be exactly 10 digits.");
        if (!recordExists("apartment", "apartment_id", apartmentId))
            throw new ValidationException("Apartment ID " + apartmentId + " does not exist.");

        String sql = "INSERT INTO resident (name, contact, apartment_id) VALUES (?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.setString(2, contact);
            ps.setInt(3, apartmentId);
            ps.executeUpdate();
            // a resident moving in makes the apartment occupied
            try (PreparedStatement up = con.prepareStatement(
                    "UPDATE apartment SET occupancy='Occupied' WHERE apartment_id=?")) {
                up.setInt(1, apartmentId);
                up.executeUpdate();
            }
            return generatedId(ps);
        }
    }

    public List<Resident> listResidents() throws SQLException {
        List<Resident> list = new ArrayList<>();
        String sql = "SELECT r.resident_id, r.name, r.contact, r.apartment_id, a.apartment_no "
                   + "FROM resident r JOIN apartment a ON r.apartment_id = a.apartment_id "
                   + "ORDER BY r.resident_id";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Resident(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getInt(4), rs.getString(5)));
            }
        }
        return list;
    }

    // ------------------------------------------------------------------ payments

    public int addMonthlyCharge(int apartmentId, String month, double amount)
            throws SQLException, ValidationException {
        try {
            YearMonth.parse(month);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ValidationException("Month must be in YYYY-MM format, e.g. 2026-12.");
        }
        if (amount <= 0) throw new ValidationException("Amount must be greater than zero.");
        if (!recordExists("apartment", "apartment_id", apartmentId))
            throw new ValidationException("Apartment ID " + apartmentId + " does not exist.");

        String sql = "INSERT INTO maintenance_payment (apartment_id, month, amount, status) VALUES (?,?,?,'Pending')";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, apartmentId);
            ps.setString(2, month);
            ps.setDouble(3, amount);
            ps.executeUpdate();
            return generatedId(ps);
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                throw new ValidationException("A charge for this apartment and month already exists.");
            }
            throw e;
        }
    }

    public void markPaid(int paymentId) throws SQLException, ValidationException {
        String check = "SELECT status FROM maintenance_payment WHERE payment_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(check)) {
            ps.setInt(1, paymentId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new ValidationException("Payment ID " + paymentId + " does not exist.");
            if ("Paid".equals(rs.getString(1))) throw new ValidationException("Payment is already marked Paid.");
        }
        String sql = "UPDATE maintenance_payment SET status='Paid', paid_date=? WHERE payment_id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, paymentId);
            ps.executeUpdate();
        }
    }

    public List<MaintenancePayment> listPayments(String statusFilter) throws SQLException {
        List<MaintenancePayment> list = new ArrayList<>();
        String sql = "SELECT p.payment_id, p.apartment_id, a.apartment_no, p.month, p.amount, p.status, p.paid_date "
                   + "FROM maintenance_payment p JOIN apartment a ON p.apartment_id = a.apartment_id "
                   + (statusFilter == null ? "" : "WHERE p.status = ? ")
                   + "ORDER BY p.month, a.apartment_no";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (statusFilter != null) ps.setString(1, statusFilter);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new MaintenancePayment(rs.getInt(1), rs.getInt(2), rs.getString(3),
                        rs.getString(4), rs.getDouble(5), rs.getString(6), rs.getString(7)));
            }
        }
        return list;
    }

    // ------------------------------------------------------------------ complaints

    public int registerComplaint(int residentId, String category, String description, String date)
            throws SQLException, ValidationException {
        if (!CATEGORIES.contains(category))
            throw new ValidationException("Category must be one of " + CATEGORIES);
        if (description == null || description.trim().isEmpty())
            throw new ValidationException("Description is required.");
        if (!recordExists("resident", "resident_id", residentId))
            throw new ValidationException("Resident ID " + residentId + " does not exist.");
        String complaintDate = (date == null || date.trim().isEmpty()) ? LocalDate.now().toString() : date.trim();
        try {
            LocalDate.parse(complaintDate);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Date must be in YYYY-MM-DD format.");
        }

        String sql = "INSERT INTO complaint (resident_id, category, description, complaint_date, status) "
                   + "VALUES (?,?,?,?,'Open')";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, residentId);
            ps.setString(2, category);
            ps.setString(3, description.trim());
            ps.setString(4, complaintDate);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    public void updateComplaintStatus(int complaintId, String newStatus)
            throws SQLException, ValidationException {
        if (!STATUSES.contains(newStatus))
            throw new ValidationException("Status must be one of " + STATUSES);
        String current;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT status FROM complaint WHERE complaint_id=?")) {
            ps.setInt(1, complaintId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new ValidationException("Complaint ID " + complaintId + " does not exist.");
            current = rs.getString(1);
        }
        if (current.equals("Resolved") && !newStatus.equals("Resolved"))
            throw new ValidationException("A resolved complaint cannot be reopened.");

        String sql = "UPDATE complaint SET status=?, resolved_date=? WHERE complaint_id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, newStatus.equals("Resolved") ? LocalDate.now().toString() : null);
            ps.setInt(3, complaintId);
            ps.executeUpdate();
        }
    }

    public List<Complaint> listComplaints(String statusFilter) throws SQLException {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT c.complaint_id, c.resident_id, r.name, c.category, c.description, "
                   + "c.complaint_date, c.status, c.resolved_date "
                   + "FROM complaint c JOIN resident r ON c.resident_id = r.resident_id "
                   + (statusFilter == null ? "" : "WHERE c.status = ? ")
                   + "ORDER BY c.complaint_date, c.complaint_id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (statusFilter != null) ps.setString(1, statusFilter);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Complaint(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)));
            }
        }
        return list;
    }

    // ------------------------------------------------------------------ reports (collections + streams)

    /** Total pending amount for each apartment, highest first. */
    public Map<String, Double> pendingAmountByApartment() throws SQLException {
        return listPayments("Pending").stream()
                .collect(Collectors.groupingBy(MaintenancePayment::getApartmentNo,
                        TreeMap::new, Collectors.summingDouble(MaintenancePayment::getAmount)));
    }

    /** Number of complaints in each category. */
    public Map<String, Long> complaintsByCategory() throws SQLException {
        return listComplaints(null).stream()
                .collect(Collectors.groupingBy(Complaint::getCategory,
                        TreeMap::new, Collectors.counting()));
    }

    /** Number of complaints in each status. */
    public Map<String, Long> complaintsByStatus() throws SQLException {
        return listComplaints(null).stream()
                .collect(Collectors.groupingBy(Complaint::getStatus,
                        TreeMap::new, Collectors.counting()));
    }

    /** Average days taken to resolve complaints, per category. */
    public Map<String, Double> averageResolutionDays() throws SQLException {
        return listComplaints("Resolved").stream()
                .filter(c -> c.getResolvedDate() != null)
                .collect(Collectors.groupingBy(Complaint::getCategory, TreeMap::new,
                        Collectors.averagingLong(c -> java.time.temporal.ChronoUnit.DAYS.between(
                                LocalDate.parse(c.getComplaintDate()), LocalDate.parse(c.getResolvedDate())))));
    }

    // ------------------------------------------------------------------ helpers

    private boolean recordExists(String table, String idColumn, int id) throws SQLException {
        String sql = "SELECT 1 FROM " + table + " WHERE " + idColumn + " = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeQuery().next();
        }
    }

    private int generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            return keys.next() ? keys.getInt(1) : -1;
        }
    }
}
