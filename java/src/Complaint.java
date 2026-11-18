/** A complaint registered by a resident. */
public class Complaint {
    private int complaintId;
    private int residentId;
    private String residentName;   // filled from a join, for display
    private String category;       // Water, Electrical, Cleanliness, Other
    private String description;
    private String complaintDate;  // YYYY-MM-DD
    private String status;         // Open, In Progress, Resolved
    private String resolvedDate;   // null until Resolved

    public Complaint(int complaintId, int residentId, String residentName, String category,
                     String description, String complaintDate, String status, String resolvedDate) {
        this.complaintId = complaintId;
        this.residentId = residentId;
        this.residentName = residentName;
        this.category = category;
        this.description = description;
        this.complaintDate = complaintDate;
        this.status = status;
        this.resolvedDate = resolvedDate;
    }

    public int getComplaintId()      { return complaintId; }
    public int getResidentId()       { return residentId; }
    public String getResidentName()  { return residentName; }
    public String getCategory()      { return category; }
    public String getDescription()   { return description; }
    public String getComplaintDate() { return complaintDate; }
    public String getStatus()        { return status; }
    public String getResolvedDate()  { return resolvedDate; }

    @Override
    public String toString() {
        return String.format("%-4d %-16s %-12s %-10s %-12s %s",
                complaintId, residentName, category, complaintDate, status, description);
    }
}
