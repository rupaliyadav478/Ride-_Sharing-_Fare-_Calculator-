/** Pricing rules for one vehicle type (a row of vehicle_types). */
public class VehicleRate {
    public final int id;
    public final String name;
    public final double baseFare, perKm, perMinute, minFare;
    public final int seats;

    public VehicleRate(int id, String name, double baseFare, double perKm,
                       double perMinute, double minFare, int seats) {
        this.id = id; this.name = name; this.baseFare = baseFare;
        this.perKm = perKm; this.perMinute = perMinute;
        this.minFare = minFare; this.seats = seats;
    }
}
