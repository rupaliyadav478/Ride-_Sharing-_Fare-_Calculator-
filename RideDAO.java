import java.sql.*;
import java.time.LocalTime;
import java.util.*;

public class RideDAO {

    private static final String SELECT =
        "SELECT r.*, v.name AS vehicle_name, "
      + "TIMESTAMPDIFF(MINUTE, r.created_at, NOW()) AS mins_since_booking "
      + "FROM rides r JOIN vehicle_types v ON v.vehicle_id = r.vehicle_id ";

    private Ride map(ResultSet rs) throws SQLException {
        Ride r = new Ride();
        r.id = rs.getInt("ride_id");
        r.userId = rs.getInt("user_id");
        r.vehicleName = rs.getString("vehicle_name");
        r.pickup = rs.getString("pickup_location");
        r.destination = rs.getString("destination");
        r.waitMin = rs.getInt("waiting_min");
        r.pickupTime = rs.getTime("pickup_time").toLocalTime();
        r.status = rs.getString("status");
        r.createdAt = rs.getTimestamp("created_at");
        r.cancellationFee = rs.getDouble("cancellation_fee");
        r.minsSinceBooking = rs.getInt("mins_since_booking");

        FareBreakdown f = r.fare;
        f.vehicleName = r.vehicleName;
        f.distanceKm = rs.getDouble("distance_km");
        f.durationMin = rs.getInt("duration_min");
        f.baseFare = rs.getDouble("base_fare");
        f.distanceCharge = rs.getDouble("distance_charge");
        f.timeCharge = rs.getDouble("time_charge");
        f.minFareAdjust = rs.getDouble("min_fare_adjust");
        f.surgeMultiplier = rs.getDouble("surge_multiplier");
        f.surgeCharge = rs.getDouble("surge_charge");
        f.nightCharge = rs.getDouble("night_charge");
        f.waitingCharge = rs.getDouble("waiting_charge");
        f.promoCode = rs.getString("promo_code");
        f.discount = rs.getDouble("discount");
        f.gst = rs.getDouble("gst");
        f.total = rs.getDouble("total_fare");
        return r;
    }

    /** Confirms a booking: stores the ride with status BOOKED and returns its ID. */
    public int book(int userId, int vehicleId, String pickup, String destination,
                    int waitMin, LocalTime pickupTime, FareBreakdown f) throws SQLException {
        String sql = "INSERT INTO rides (user_id, vehicle_id, pickup_location, destination, "
                   + "distance_km, duration_min, waiting_min, pickup_time, base_fare, distance_charge, "
                   + "time_charge, min_fare_adjust, surge_multiplier, surge_charge, night_charge, "
                   + "waiting_charge, promo_code, discount, gst, total_fare) "
                   + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setInt(i++, userId);
            ps.setInt(i++, vehicleId);
            ps.setString(i++, pickup);
            ps.setString(i++, destination);
            ps.setDouble(i++, f.distanceKm);
            ps.setInt(i++, f.durationMin);
            ps.setInt(i++, waitMin);
            ps.setTime(i++, Time.valueOf(pickupTime.withNano(0)));
            ps.setDouble(i++, f.baseFare);
            ps.setDouble(i++, f.distanceCharge);
            ps.setDouble(i++, f.timeCharge);
            ps.setDouble(i++, f.minFareAdjust);
            ps.setDouble(i++, f.surgeMultiplier);
            ps.setDouble(i++, f.surgeCharge);
            ps.setDouble(i++, f.nightCharge);
            ps.setDouble(i++, f.waitingCharge);
            ps.setString(i++, f.promoCode);          // null -> SQL NULL
            ps.setDouble(i++, f.discount);
            ps.setDouble(i++, f.gst);
            ps.setDouble(i++, f.total);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getInt(1);
            }
        }
    }

    /** All rides of a user, newest first. */
    public List<Ride> findByUser(int userId) throws SQLException {
        List<Ride> list = new ArrayList<>();
        String sql = SELECT + "WHERE r.user_id = ? ORDER BY r.ride_id DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** A ride by ID, only if it belongs to this user (null otherwise). */
    public Ride findByIdAndUser(int rideId, int userId) throws SQLException {
        String sql = SELECT + "WHERE r.ride_id = ? AND r.user_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Cancels a BOOKED ride. Returns false if it was not in BOOKED state. */
    public boolean cancel(int rideId, int userId, double fee) throws SQLException {
        String sql = "UPDATE rides SET status = 'CANCELLED', cancellation_fee = ?, cancelled_at = NOW() "
                   + "WHERE ride_id = ? AND user_id = ? AND status = 'BOOKED'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, fee);
            ps.setInt(2, rideId);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Marks a BOOKED ride as COMPLETED (simulates the trip ending). */
    public boolean complete(int rideId, int userId) throws SQLException {
        String sql = "UPDATE rides SET status = 'COMPLETED' "
                   + "WHERE ride_id = ? AND user_id = ? AND status = 'BOOKED'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
