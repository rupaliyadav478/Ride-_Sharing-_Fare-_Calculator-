import java.time.LocalTime;

/** Pure fare logic - no database access here. */
public class FareCalculator {
    static final double GST_RATE = 0.05;
    static final double NIGHT_RATE = 0.25;      // 11 PM - 5 AM
    static final int FREE_WAIT_MIN = 3;
    static final double WAIT_PER_MIN = 2.0;

    public static final int CANCEL_FREE_MIN = 2;   // free cancellation window
    public static final double CANCEL_FEE = 25.0;  // after that
    public static final double AVG_SPEED_KMH = 25.0;

    /** Surge from demand (requests / available drivers) plus peak-hour bump, capped at 2.5. */
    public static double surgeMultiplier(double demandRatio, LocalTime time) {
        double m;
        if (demandRatio < 1.0)      m = 1.0;
        else if (demandRatio < 1.5) m = 1.25;
        else if (demandRatio < 2.0) m = 1.5;
        else                        m = 2.0;
        int h = time.getHour();
        if ((h >= 8 && h < 10) || (h >= 17 && h < 20)) m += 0.2;
        return Math.min(m, 2.5);
    }

    static boolean isNight(LocalTime t) {
        return t.getHour() >= 23 || t.getHour() < 5;
    }

    public static FareBreakdown calculate(VehicleRate v, double km, int minutes, int waitMin,
                                          LocalTime time, double demandRatio, Promo promo) {
        if (km <= 0 || minutes <= 0)
            throw new IllegalArgumentException("Distance and duration must be positive.");

        FareBreakdown f = new FareBreakdown();
        f.vehicleName = v.name;
        f.distanceKm = km;
        f.durationMin = minutes;
        f.baseFare = v.baseFare;
        f.distanceCharge = km * v.perKm;
        f.timeCharge = minutes * v.perMinute;

        double raw = f.baseFare + f.distanceCharge + f.timeCharge;
        double fare = Math.max(raw, v.minFare);
        f.minFareAdjust = fare - raw;

        f.surgeMultiplier = surgeMultiplier(demandRatio, time);
        f.surgeCharge = fare * (f.surgeMultiplier - 1.0);
        f.nightCharge = isNight(time) ? fare * NIGHT_RATE : 0;
        f.waitingCharge = Math.max(0, waitMin - FREE_WAIT_MIN) * WAIT_PER_MIN;

        double subtotal = fare + f.surgeCharge + f.nightCharge + f.waitingCharge;
        if (promo != null) {
            f.discount = promo.discountFor(subtotal);
            f.promoCode = promo.code;
        }
        double taxable = subtotal - f.discount;
        f.gst = taxable * GST_RATE;
        f.total = round2(taxable + f.gst);

        f.distanceCharge = round2(f.distanceCharge);
        f.timeCharge = round2(f.timeCharge);
        f.minFareAdjust = round2(f.minFareAdjust);
        f.surgeCharge = round2(f.surgeCharge);
        f.nightCharge = round2(f.nightCharge);
        f.discount = round2(f.discount);
        f.gst = round2(f.gst);
        return f;
    }

    public static double round2(double x) {
        return Math.round(x * 100.0) / 100.0;
    }
}
