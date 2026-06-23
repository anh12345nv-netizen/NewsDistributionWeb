package test;
import java.sql.*;

public class TestDb {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:sqlserver://localhost:1234;databaseName=NewsDistributionWeb;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true";
        String user = "sa";
        String pass = "YourPassword123";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Checking sync_log...");
            String sql1 = "SELECT TOP 5 * FROM sync_log ORDER BY id DESC";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql1)) {
                while (rs.next()) {
                    System.out.println(rs.getString("table_name") + " - " + rs.getString("status") + " - " + rs.getString("error_msg"));
                }
            }
            
            System.out.println("Checking m_hoa_don WEB revenue...");
            String sql2 = "SELECT source, SUM(tong_tien) as revenue, COUNT(*) as cnt FROM m_hoa_don GROUP BY source";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql2)) {
                while (rs.next()) {
                    System.out.println(rs.getString("source") + " - Rev: " + rs.getObject("revenue") + " - Count: " + rs.getInt("cnt"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
