// Place this file in the 'src' folder
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521/XE";
    private static final String USER = "system";

    // ‼️ VERY IMPORTANT: Replace this with your actual Oracle password!
    private static final String PASSWORD = "dev123";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}