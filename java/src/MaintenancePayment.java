/** Monthly maintenance charge for one apartment, with its payment status. */
public class MaintenancePayment {
    private int paymentId;
    private int apartmentId;
    private String apartmentNo;
    private String month;        // YYYY-MM
    private double amount;
    private String status;       // Paid, Pending
    private String paidDate;     // null while Pending

    public MaintenancePayment(int paymentId, int apartmentId, String apartmentNo,
                              String month, double amount, String status, String paidDate) {
        this.paymentId = paymentId;
        this.apartmentId = apartmentId;
        this.apartmentNo = apartmentNo;
        this.month = month;
        this.amount = amount;
        this.status = status;
        this.paidDate = paidDate;
    }

    public int getPaymentId()      { return paymentId; }
    public int getApartmentId()    { return apartmentId; }
    public String getApartmentNo() { return apartmentNo; }
    public String getMonth()       { return month; }
    public double getAmount()      { return amount; }
    public String getStatus()      { return status; }
    public String getPaidDate()    { return paidDate; }

    @Override
    public String toString() {
        return String.format("%-4d %-8s %-8s %-9.2f %-8s %-10s",
                paymentId, apartmentNo, month, amount, status,
                paidDate == null ? "-" : paidDate);
    }
}
