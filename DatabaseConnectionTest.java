import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionTest {
    
    public static void main(String[] args) {
        // Database configurations
        String[] databases = {
            "orders_db", "payments_db", "inventory_db"
        };
        
        String[] users = {
            "orders_user", "payments_user", "inventory_user"
        };
        
        String[] passwords = {
            "orders_pass", "payments_pass", "inventory_pass"
        };
        
        String host = "localhost";
        String port = "5432";
        
        System.out.println("Testing PostgreSQL connections...\n");
        
        for (int i = 0; i < databases.length; i++) {
            testConnection(host, port, databases[i], users[i], passwords[i]);
        }
    }
    
    private static void testConnection(String host, String port, String database, String username, String password) {
        String url = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
        
        try {
            System.out.printf("Testing connection to %s... ", database);
            
            Connection connection = DriverManager.getConnection(url, username, password);
            
            if (connection != null && !connection.isClosed()) {
                System.out.println("✓ SUCCESS");
                connection.close();
            } else {
                System.out.println("✗ FAILED - Connection is null or closed");
            }
            
        } catch (SQLException e) {
            System.out.printf("✗ FAILED - %s\n", e.getMessage());
        }
    }
}
