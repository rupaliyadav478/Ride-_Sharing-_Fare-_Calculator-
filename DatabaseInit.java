import java.sql.*;

/**
 * Runs once at startup: creates the database tables if they are missing
 * and seeds default vehicle rates / promo codes if those tables are empty.
 * (sql/schema.sql does the same thing and can still be used manually.)
 */
public class DatabaseInit {

    public static void seedIfEmpty() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement()) {

            try (ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
                if (rs.next()) System.out.println("Connected to database: " + rs.getString(1));
            }

            createTables(st);

            if (count(st, "vehicle_types") == 0) {
                st.executeUpdate("INSERT INTO vehicle_types (name, base_fare, per_km, per_minute, min_fare, seats) VALUES "
                        + "('Bike',20,6.0,0.50,30,1),"
                        + "('Auto-rickshaw',30,9.0,0.75,50,3),"
                        + "('Mini/Sedan',50,12.0,1.10,90,4),"
                        + "('SUV',90,18.0,1.75,150,6)");
                System.out.println("Default vehicle rates added.");
            }

            if (count(st, "promo_codes") == 0) {
                st.executeUpdate("INSERT INTO promo_codes (code, discount_type, discount_value, max_discount, expiry_date) VALUES "
                        + "('FIRST50','PERCENT',50,100,NULL),"
                        + "('SAVE20','PERCENT',20,80,NULL),"
                        + "('WELCOME30','FLAT',30,30,NULL)");
                System.out.println("Default promo codes added.");
            }
        }
    }

    private static void createTables(Statement st) throws SQLException {
        st.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
            + "user_id INT AUTO_INCREMENT PRIMARY KEY, "
            + "full_name VARCHAR(60) NOT NULL, "
            + "username VARCHAR(20) NOT NULL UNIQUE, "
            + "phone VARCHAR(10) NOT NULL UNIQUE, "
            + "password_hash VARCHAR(150) NOT NULL, "
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        st.executeUpdate("CREATE TABLE IF NOT EXISTS vehicle_types ("
            + "vehicle_id INT AUTO_INCREMENT PRIMARY KEY, "
            + "name VARCHAR(30) NOT NULL UNIQUE, "
            + "base_fare DECIMAL(8,2) NOT NULL, per_km DECIMAL(8,2) NOT NULL, "
            + "per_minute DECIMAL(8,2) NOT NULL, min_fare DECIMAL(8,2) NOT NULL, "
            + "seats INT NOT NULL)");

        st.executeUpdate("CREATE TABLE IF NOT EXISTS promo_codes ("
            + "code VARCHAR(20) PRIMARY KEY, "
            + "discount_type ENUM('PERCENT','FLAT') NOT NULL, "
            + "discount_value DECIMAL(8,2) NOT NULL, max_discount DECIMAL(8,2) NOT NULL, "
            + "expiry_date DATE NULL, active TINYINT(1) NOT NULL DEFAULT 1)");

        st.executeUpdate("CREATE TABLE IF NOT EXISTS rides ("
            + "ride_id INT AUTO_INCREMENT PRIMARY KEY, "
            + "user_id INT NOT NULL, vehicle_id INT NOT NULL, "
            + "pickup_location VARCHAR(100) NOT NULL, destination VARCHAR(100) NOT NULL, "
            + "distance_km DECIMAL(8,2) NOT NULL, duration_min INT NOT NULL, "
            + "waiting_min INT NOT NULL DEFAULT 0, pickup_time TIME NOT NULL, "
            + "base_fare DECIMAL(10,2) NOT NULL, distance_charge DECIMAL(10,2) NOT NULL, "
            + "time_charge DECIMAL(10,2) NOT NULL, min_fare_adjust DECIMAL(10,2) NOT NULL DEFAULT 0, "
            + "surge_multiplier DECIMAL(4,2) NOT NULL DEFAULT 1, "
            + "surge_charge DECIMAL(10,2) NOT NULL DEFAULT 0, "
            + "night_charge DECIMAL(10,2) NOT NULL DEFAULT 0, "
            + "waiting_charge DECIMAL(10,2) NOT NULL DEFAULT 0, "
            + "promo_code VARCHAR(20) NULL, discount DECIMAL(10,2) NOT NULL DEFAULT 0, "
            + "gst DECIMAL(10,2) NOT NULL, total_fare DECIMAL(10,2) NOT NULL, "
            + "status ENUM('BOOKED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'BOOKED', "
            + "cancellation_fee DECIMAL(10,2) NOT NULL DEFAULT 0, "
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
            + "cancelled_at TIMESTAMP NULL, "
            + "FOREIGN KEY (user_id) REFERENCES users(user_id), "
            + "FOREIGN KEY (vehicle_id) REFERENCES vehicle_types(vehicle_id))");
    }

    private static int count(Statement st, String table) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
