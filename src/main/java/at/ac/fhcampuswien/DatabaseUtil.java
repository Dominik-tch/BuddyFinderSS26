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
            user_limit INT NOT NULL
        );
        """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(createActivitiesTable);
            System.out.println("Tables initialized successfully.");

        } catch (SQLException e) {
            throw new DatabaseException("Error attempting to connect to the BuddyFinder database", e);
        }
    }
}
