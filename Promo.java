/** A promo code (a row of promo_codes). */
public class Promo {
    public final String code, type;          // type = PERCENT or FLAT
    public final double value, maxDiscount;

    public Promo(String code, String type, double value, double maxDiscount) {
        this.code = code; this.type = type; this.value = value; this.maxDiscount = maxDiscount;
    }

    public double discountFor(double amount) {
        double d = type.equals("PERCENT") ? amount * value / 100.0 : value;
        return Math.min(Math.min(d, maxDiscount), amount);
    }
}
