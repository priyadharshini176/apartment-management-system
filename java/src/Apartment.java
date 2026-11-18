/** Represents one apartment unit in the complex. */
public class Apartment {
    private int apartmentId;
    private String blockNo;
    private String apartmentNo;
    private String apartmentType;   // 1BHK, 2BHK, 3BHK
    private String occupancy;       // Occupied, Vacant

    public Apartment(int apartmentId, String blockNo, String apartmentNo,
                     String apartmentType, String occupancy) {
        this.apartmentId = apartmentId;
        this.blockNo = blockNo;
        this.apartmentNo = apartmentNo;
        this.apartmentType = apartmentType;
        this.occupancy = occupancy;
    }

    public int getApartmentId()      { return apartmentId; }
    public String getBlockNo()       { return blockNo; }
    public String getApartmentNo()   { return apartmentNo; }
    public String getApartmentType() { return apartmentType; }
    public String getOccupancy()     { return occupancy; }

    @Override
    public String toString() {
        return String.format("%-4d %-6s %-8s %-6s %-9s",
                apartmentId, blockNo, apartmentNo, apartmentType, occupancy);
    }
}
