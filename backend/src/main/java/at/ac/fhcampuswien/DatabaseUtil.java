package at.ac.fhcampuswien;

import at.ac.fhcampuswien.exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtil {
    private static final String JDBC_URL = "jdbc:h2:~/BuddyFinderStorage";
    private static final String USER = "Admin";
    private static final String PASSWORD = "1234";
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    }
    public static void initializeDatabase() {
        String createActivitiesTable = """
            CREATE TABLE IF NOT EXISTS activities (
                id UUID PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                owner VARCHAR(255) NOT NULL,
                price INT NOT NULL,
                location VARCHAR(255) NOT NULL,
                user_limit INT NOT NULL,
                description VARCHAR(255),
                latitude VARCHAR(255),
                longitude VARCHAR(255),
                weather VARCHAR(255),
                activityDate TEXT,
                activityTime TEXT
            );
        """;
        String createUserTable = """
            CREATE TABLE IF NOT EXISTS users (
                id UUID PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                first_name VARCHAR(50),
                last_name VARCHAR(50)
            );
            """;
        String createSessionTable = """
            CREATE TABLE IF NOT EXISTS sessions (
                session_id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(36) NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );
            """;
        String createActivityParticipants = """
            CREATE TABLE IF NOT EXISTS activity_participants (
                user_id UUID NOT NULL,
                activity_id UUID NOT NULL,
                PRIMARY KEY (user_id, activity_id),
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE
            );
            """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            // THE BULLDOZER: Wipes the old database clean
            //statement.execute("DROP ALL OBJECTS");

            statement.execute(createActivitiesTable);
            statement.execute(createUserTable);
            statement.execute(createSessionTable);
            statement.execute(createActivityParticipants);
            System.out.println("Tables initialized successfully.");

        } catch (SQLException e) {
            throw new DatabaseException("Error attempting to connect to the BuddyFinder database", e);
        }
    }
}
