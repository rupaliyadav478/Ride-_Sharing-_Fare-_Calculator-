/** A registered user (a row of users). */
public class User {
    public final int id;
    public final String fullName, username, phone, passwordHash, createdAt;

    public User(int id, String fullName, String username, String phone,
                String passwordHash, String createdAt) {
        this.id = id; this.fullName = fullName; this.username = username;
        this.phone = phone; this.passwordHash = passwordHash; this.createdAt = createdAt;
    }
}
