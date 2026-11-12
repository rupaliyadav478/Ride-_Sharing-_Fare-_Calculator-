import java.sql.*;
import java.util.*;

public class VehicleDAO {
    public List<VehicleRate> findAll() throws SQLException {
        List<VehicleRate> list = new ArrayList<>();
        String sql = "SELECT * FROM vehicle_types ORDER BY vehicle_id";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new VehicleRate(rs.getInt("vehicle_id"), rs.getString("name"),
                        rs.getDouble("base_fare"), rs.getDouble("per_km"),
                        rs.getDouble("per_minute"), rs.getDouble("min_fare"), rs.getInt("seats")));
            }
        }
        return list;
    }
}
