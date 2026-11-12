import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Creates JDBC connections. Override defaults with env vars DB_URL, DB_USER, DB_PASS. */
public class DBConnection {
    private static final String URL = env("DB_URL",
            "jdbc:mysql://localhost:3306/ride_fare_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String USER = env("DB_USER", "root");
    private static final String PASS = env("DB_PASS", "root");

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? def : v;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
