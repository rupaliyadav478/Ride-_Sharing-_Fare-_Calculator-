import java.io.Console;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.*;

/** Console UI. Run: java -cp "out:lib/*" RideFareApp */
public class RideFareApp {
    private static final Scanner sc = new Scanner(System.in);
    private static final UserDAO userDAO = new UserDAO();
    private static final VehicleDAO vehicleDAO = new VehicleDAO();
    private static final PromoDAO promoDAO = new PromoDAO();
    private static final RideDAO rideDAO = new RideDAO();

    /** Everything the user entered + the calculated fare, before booking. */
    private static class Quote {
        VehicleRate vehicle;
        String pickup, destination;
        int waitMin;
        LocalTime time;
        FareBreakdown fare;
    }

    // =====================================================
    //  MAIN: auth menu -> user menu
    // =====================================================
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("   RIDE SHARING FARE CALCULATOR (JDBC)");
        System.out.println("===========================================");

        try {
            DatabaseInit.seedIfEmpty();
        } catch (SQLException e) {
            System.out.println("Database setup problem: " + e.getMessage());
        }

        boolean running = true;
        while (running) {
            System.out.println("\n1. Register");
            System.out.println("2. Login");
            System.out.println("0. Exit");
            int choice = readInt("Choose option: ", 0, 2);
            try {
                switch (choice) {
                    case 1: register(); break;
                    case 2:
                        User u = login();
                        if (u != null) userMenu(u);
                        break;
                    default: running = false;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
        System.out.println("Thank you for riding with us!");
    }

    private static void userMenu(User user) {
        System.out.println("\nWelcome, " + user.fullName + "!");
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n1. Fare estimate (no booking)");
            System.out.println("2. Book a ride");
            System.out.println("3. My rides (history)");
            System.out.println("4. View receipt");
            System.out.println("5. Cancel a ride");
            System.out.println("6. Mark ride as completed");
            System.out.println("7. Rate card");
            System.out.println("8. My profile");
            System.out.println("0. Logout");
            int choice = readInt("Choose option: ", 0, 8);
            try {
                switch (choice) {
                    case 1: estimateOnly(); break;
                    case 2: bookRide(user); break;
                    case 3: history(user); break;
                    case 4: receipt(user); break;
                    case 5: cancelRide(user); break;
                    case 6: completeRide(user); break;
                    case 7: rateCard(); break;
                    case 8: profile(user); break;
                    default: loggedIn = false;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Logged out.");
    }

    // =====================================================
    //  1. Registration & login
    // =====================================================
    private static void register() throws SQLException {
        System.out.println("\n--- New account ---");
        String name = readText("Full name : ");
        String username;
        while (true) {
            username = readText("Username (4-20 letters/digits/_): ");
            if (username.matches("\\w{4,20}")) break;
            System.out.println("  Invalid username.");
        }
        String phone;
        while (true) {
            phone = readText("Phone (10 digits): ");
            if (phone.matches("\\d{10}")) break;
            System.out.println("  Phone must be exactly 10 digits.");
        }
        String pass;
        while (true) {
            pass = readPassword("Password (min 6 chars): ");
            if (pass.length() < 6) { System.out.println("  Too short."); continue; }
            if (pass.equals(readPassword("Confirm password: "))) break;
            System.out.println("  Passwords do not match.");
        }
        try {
            userDAO.register(name, username, phone, PasswordUtil.hash(pass));
            System.out.println("Account created. You can login now.");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) System.out.println("Username or phone is already registered.");
            else throw e;
        }
    }

    private static User login() throws SQLException {
        System.out.println("\n--- Login ---");
        for (int attempt = 1; attempt <= 3; attempt++) {
            String username = readText("Username: ");
            String pass = readPassword("Password: ");
            User u = userDAO.findByUsername(username);
            if (u != null && PasswordUtil.verify(pass, u.passwordHash)) return u;
            System.out.println("Wrong username or password. (" + attempt + "/3)");
        }
        System.out.println("Too many failed attempts.");
        return null;
    }

    private static void profile(User u) {
        System.out.println("\nName        : " + u.fullName);
        System.out.println("Username    : " + u.username);
        System.out.println("Phone       : " + u.phone);
        System.out.println("Member since: " + u.createdAt);
    }

    // =====================================================
    //  2 + 3. Ride type, fare calculation
    // =====================================================
    private static Quote buildQuote() throws SQLException {
        Quote q = new Quote();
        q.vehicle = chooseVehicle();
        q.pickup = readText("Pickup location: ");
        q.destination = readText("Destination: ");
        double km = readDouble("Distance (km): ", 0.1, 1000);
        int est = Math.max(1, (int) Math.round(km / FareCalculator.AVG_SPEED_KMH * 60));
        int min = readIntDefault("Ride duration in min [Enter = " + est + "]: ", est, 1, 1440);
        q.waitMin = readIntDefault("Expected waiting at pickup in min [Enter = 0]: ", 0, 0, 120);
        q.time = readTime();
        double demand = chooseDemand();

        Promo promo = null;
        System.out.print("Promo code (Enter to skip): ");
        String code = sc.nextLine().trim();
        if (!code.isEmpty()) {
            promo = promoDAO.findValid(code);
            if (promo == null) System.out.println("  Invalid or expired promo - continuing without it.");
        }
        q.fare = FareCalculator.calculate(q.vehicle, km, min, q.waitMin, q.time, demand, promo);
        return q;
    }

    private static void showQuote(Quote q) {
        System.out.println("\n========== ESTIMATED FARE ==========");
        System.out.printf("Vehicle : %s%n", q.vehicle.name);
        System.out.printf("Route   : %s -> %s%n", q.pickup, q.destination);
        System.out.println("-------------------------------------------");
        q.fare.printLines();
        System.out.println("===========================================");
    }

    private static void estimateOnly() throws SQLException {
        showQuote(buildQuote());
    }

    // =====================================================
    //  4. Booking, history, cancel
    // =====================================================
    private static void bookRide(User user) throws SQLException {
        Quote q = buildQuote();
        showQuote(q);
        System.out.print("Confirm booking? (y/n): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("Booking discarded.");
            return;
        }
        int id = rideDAO.book(user.id, q.vehicle.id, q.pickup, q.destination,
                q.waitMin, q.time, q.fare);
        System.out.println("Ride booked! Booking ID = " + id);
        Ride r = rideDAO.findByIdAndUser(id, user.id);
        if (r != null) r.printReceipt(user.fullName);
    }

    private static void history(User user) throws SQLException {
        List<Ride> rides = rideDAO.findByUser(user.id);
        if (rides.isEmpty()) { System.out.println("No rides yet."); return; }
        System.out.printf("%n%-5s %-19s %-14s %-30s %9s %-10s%n",
                "ID", "Booked on", "Vehicle", "Route", "Fare", "Status");
        for (Ride r : rides) {
            String route = cut(r.pickup, 12) + " -> " + cut(r.destination, 12);
            double shown = r.status.equals("CANCELLED") ? r.cancellationFee : r.fare.total;
            System.out.printf("%-5d %-19s %-14s %-30s %9.2f %-10s%n",
                    r.id, r.bookedOn(), cut(r.vehicleName, 14), route, shown, r.status);
        }
        System.out.println("(Fare for CANCELLED rides = cancellation fee)");
    }

    private static void receipt(User user) throws SQLException {
        int id = readInt("Ride ID: ", 1, Integer.MAX_VALUE);
        Ride r = rideDAO.findByIdAndUser(id, user.id);
        if (r == null) System.out.println("No such ride on your account.");
        else r.printReceipt(user.fullName);
    }

    private static void cancelRide(User user) throws SQLException {
        int id = readInt("Ride ID to cancel: ", 1, Integer.MAX_VALUE);
        Ride r = rideDAO.findByIdAndUser(id, user.id);
        if (r == null) { System.out.println("No such ride on your account."); return; }
        if (!r.status.equals("BOOKED")) {
            System.out.println("Only BOOKED rides can be cancelled (this ride is " + r.status + ").");
            return;
        }
        double fee = r.minsSinceBooking <= FareCalculator.CANCEL_FREE_MIN ? 0 : FareCalculator.CANCEL_FEE;
        System.out.printf("Cancellation fee: Rs. %.2f (free within %d min of booking)%n",
                fee, FareCalculator.CANCEL_FREE_MIN);
        System.out.print("Cancel this ride? (y/n): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("y")) { System.out.println("Not cancelled."); return; }
        if (rideDAO.cancel(id, user.id, fee)) System.out.println("Ride " + id + " cancelled.");
        else System.out.println("Could not cancel - ride status changed.");
    }

    private static void completeRide(User user) throws SQLException {
        int id = readInt("Ride ID to mark completed: ", 1, Integer.MAX_VALUE);
        if (rideDAO.complete(id, user.id)) System.out.println("Ride " + id + " marked COMPLETED.");
        else System.out.println("Ride not found or not in BOOKED state.");
    }

    private static void rateCard() throws SQLException {
        System.out.printf("%n  %-14s %6s %6s %7s %8s %6s%n", "Vehicle", "Base", "/km", "/min", "MinFare", "Seats");
        System.out.println("  ---------------------------------------------------");
        for (VehicleRate v : vehicleDAO.findAll())
            System.out.printf("  %-14s %6.0f %6.1f %7.2f %8.0f %6d%n",
                    v.name, v.baseFare, v.perKm, v.perMinute, v.minFare, v.seats);
        System.out.println("\n  Night surcharge (11PM-5AM): +25% | Peak hours (8-10AM, 5-8PM): +0.2 surge");
        System.out.println("  Free waiting 3 min, then Rs. 2/min | GST 5%");
        System.out.printf("  Cancellation: free within %d min, else Rs. %.0f%n",
                FareCalculator.CANCEL_FREE_MIN, FareCalculator.CANCEL_FEE);
    }

    // =====================================================
    //  Input helpers
    // =====================================================
    private static VehicleRate chooseVehicle() throws SQLException {
        List<VehicleRate> all = vehicleDAO.findAll();
        if (all.isEmpty()) throw new IllegalArgumentException("No vehicles in database. Run sql/schema.sql.");
        System.out.println("\nSelect ride type:");
        for (int i = 0; i < all.size(); i++) {
            VehicleRate v = all.get(i);
            System.out.printf("  %d. %-14s (base Rs.%.0f, Rs.%.1f/km, %d seat%s)%n",
                    i + 1, v.name, v.baseFare, v.perKm, v.seats, v.seats > 1 ? "s" : "");
        }
        return all.get(readInt("Choice: ", 1, all.size()) - 1);
    }

    private static double chooseDemand() {
        System.out.println("Current demand: 1. Low  2. Normal  3. High  4. Very high");
        double[] ratios = {0.8, 1.2, 1.7, 2.2};
        return ratios[readInt("Choice: ", 1, 4) - 1];
    }

    private static String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("  This field cannot be empty.");
        }
    }

    /** Hides typing when run in a real terminal; falls back to visible input in IDEs. */
    private static String readPassword(String prompt) {
        Console c = System.console();
        if (c != null) return new String(c.readPassword(prompt));
        return readText(prompt);
    }

    private static LocalTime readTime() {
        while (true) {
            System.out.print("Pickup time (HH:mm, 24h) [Enter = now]: ");
            String s = sc.nextLine().trim();
            if (s.isEmpty()) return LocalTime.now();
            try {
                return LocalTime.parse(s.length() == 4 ? "0" + s : s);
            } catch (Exception e) {
                System.out.println("  Invalid time. Example: 18:30");
            }
        }
    }

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int x = Integer.parseInt(sc.nextLine().trim());
                if (x >= min && x <= max) return x;
            } catch (NumberFormatException ignored) { }
            System.out.printf("  Enter a whole number between %d and %d.%n", min, max);
        }
    }

    private static int readIntDefault(String prompt, int def, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (s.isEmpty()) return def;
            try {
                int x = Integer.parseInt(s);
                if (x >= min && x <= max) return x;
            } catch (NumberFormatException ignored) { }
            System.out.printf("  Enter a whole number between %d and %d.%n", min, max);
        }
    }

    private static double readDouble(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            try {
                double x = Double.parseDouble(sc.nextLine().trim());
                if (x >= min && x <= max) return x;
            } catch (NumberFormatException ignored) { }
            System.out.printf("  Enter a number between %.1f and %.1f.%n", min, max);
        }
    }

    private static String cut(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "~";
    }
}
