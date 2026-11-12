import java.sql.Timestamp;
import java.time.LocalTime;

/** One booked ride (a row of rides joined with vehicle name). */
public class Ride {
    public int id, userId, waitMin, minsSinceBooking;
    public String vehicleName, pickup, destination, status;
    public LocalTime pickupTime;
    public Timestamp createdAt;
    public double cancellationFee;
    public FareBreakdown fare = new FareBreakdown();

    public String bookedOn() {
        return createdAt.toString().substring(0, 19);
    }

    public void printReceipt(String riderName) {
        System.out.println();
        System.out.println("===========================================");
        System.out.println("              RIDE RECEIPT");
        System.out.println("===========================================");
        System.out.printf("Receipt / Booking ID : %d%n", id);
        System.out.printf("Booked on            : %s%n", bookedOn());
        System.out.printf("Rider                : %s%n", riderName);
        System.out.printf("Vehicle              : %s%n", vehicleName);
        System.out.printf("Pickup               : %s%n", pickup);
        System.out.printf("Destination          : %s%n", destination);
        System.out.printf("Pickup time          : %s%n", pickupTime);
        System.out.printf("Status               : %s%n", status);
        System.out.println("-------------------------------------------");
        if (status.equals("CANCELLED")) {
            System.out.println("Ride cancelled - fare not charged.");
            System.out.printf("%-30s Rs. %8.2f%n", "CANCELLATION FEE", cancellationFee);
        } else {
            fare.printLines();
        }
        System.out.println("===========================================");
    }
}
