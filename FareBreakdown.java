/** Itemised fare - used for the estimate, the receipt and the DB row. */
public class FareBreakdown {
    public String vehicleName;
    public double distanceKm;
    public int durationMin;
    public double baseFare, distanceCharge, timeCharge, minFareAdjust;
    public double surgeMultiplier = 1.0, surgeCharge, nightCharge, waitingCharge;
    public double discount, gst, total;
    public String promoCode;                 // null if none applied

    /** Prints only the money lines (shared by the estimate and the receipt). */
    public void printLines() {
        line("Base fare", baseFare);
        line("Distance charge (" + distanceKm + " km)", distanceCharge);
        line("Time charge (" + durationMin + " min)", timeCharge);
        if (minFareAdjust > 0) line("Minimum fare adjustment", minFareAdjust);
        if (surgeCharge > 0)   line("Surge (x" + surgeMultiplier + ")", surgeCharge);
        if (nightCharge > 0)   line("Night surcharge (25%)", nightCharge);
        if (waitingCharge > 0) line("Waiting charge", waitingCharge);
        if (discount > 0)      line("Discount (" + promoCode + ")", -discount);
        line("GST (5%)", gst);
        System.out.println("-------------------------------------------");
        System.out.printf("%-30s Rs. %8.2f%n", "TOTAL FARE", total);
    }

    private void line(String name, double amount) {
        System.out.printf("%-30s Rs. %8.2f%n", name, amount);
    }
}
