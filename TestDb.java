package test;
import java.sql.*;

public class TestDb {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:sqlserver://localhost:1234;databaseName=NewsDistributionWeb;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true";
        String user = "sa";
        String pass = "YourPassword123";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            String sql = "SELECT o.id, o.order_code, o.created_at, DATEDIFF(day, o.created_at, GETDATE()) as days_unpaid, " +
            "(SELECT ISNULL(SUM(thanh_tien),0) FROM web_order_items i WHERE i.order_id = o.id) as amount " +
            "FROM web_orders o WHERE o.makh = 'DL001' AND (o.payment_status IS NULL OR o.payment_status = 'UNPAID')";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                System.out.println("Query executed successfully!");
                while (rs.next()) {
                    System.out.println(rs.getInt("id") + " - " + rs.getObject("amount"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
