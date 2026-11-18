/** Represents a resident living in an apartment. */
public class Resident {
    private int residentId;
    private String name;
    private String contact;
    private int apartmentId;
    private String apartmentNo;   // filled from a join, for display

    public Resident(int residentId, String name, String contact,
                    int apartmentId, String apartmentNo) {
        this.residentId = residentId;
        this.name = name;
        this.contact = contact;
        this.apartmentId = apartmentId;
        this.apartmentNo = apartmentNo;
    }

    public int getResidentId()     { return residentId; }
    public String getName()        { return name; }
    public String getContact()     { return contact; }
    public int getApartmentId()    { return apartmentId; }
    public String getApartmentNo() { return apartmentNo; }

    @Override
    public String toString() {
        return String.format("%-4d %-20s %-12s %-8s",
                residentId, name, contact, apartmentNo);
    }
}
