import java.sql.*;

public class PromoDAO {
    /** Returns the promo if the code exists, is active and not expired; otherwise null. */
    public Promo findValid(String code) throws SQLException {
        String sql = "SELECT * FROM promo_codes WHERE code = ? AND active = 1 "
                   + "AND (expiry_date IS NULL OR expiry_date >= CURDATE())";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Promo(rs.getString("code"), rs.getString("discount_type"),
                            rs.getDouble("discount_value"), rs.getDouble("max_discount"));
                }
            }
        }
        return null;
    }
}
