import java.sql.*;

public class UserDAO {
    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"), rs.getString("full_name"), rs.getString("username"),
                rs.getString("phone"), rs.getString("password_hash"),
                rs.getTimestamp("created_at").toString().substring(0, 19));
    }

    /** Throws SQLException with error code 1062 if username/phone already exists. */
    public void register(String fullName, String username, String phone, String passwordHash)
            throws SQLException {
        String sql = "INSERT INTO users (full_name, username, phone, password_hash) VALUES (?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, username);
            ps.setString(3, phone);
            ps.setString(4, passwordHash);
            ps.executeUpdate();
        }
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }
}
