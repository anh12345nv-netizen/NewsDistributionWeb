package test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.List;
import java.util.Map;

public class TestLogic {
    public static void main(String[] args) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        dataSource.setUrl("jdbc:sqlserver://localhost:1234;databaseName=NewsDistributionWeb;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true");
        dataSource.setUsername("sa");
        dataSource.setPassword("YourPassword123");

        JdbcTemplate jdbcB = new JdbcTemplate(dataSource);
        String makh = "DL001";
        
        try {
            Double hanMucNo = null;
            try {
                hanMucNo = jdbcB.queryForObject("SELECT han_muc_no FROM web_users WHERE makh = ?", Double.class, makh);
            } catch (Exception e) {}
            if (hanMucNo == null) hanMucNo = 0.0;
            
            System.out.println("Han muc no: " + hanMucNo);
            
            List<Map<String, Object>> unpaidOrders = jdbcB.queryForList(
                "SELECT o.id, o.order_code, o.created_at, DATEDIFF(day, o.created_at, GETDATE()) as days_unpaid, " +
                "(SELECT ISNULL(SUM(thanh_tien),0) FROM web_order_items i WHERE i.order_id = o.id) as amount " +
                "FROM web_orders o WHERE o.makh = ? AND (o.payment_status IS NULL OR o.payment_status = 'UNPAID')", makh);
                
            System.out.println("Unpaid orders count: " + unpaidOrders.size());
            
            for(Map<String, Object> m : unpaidOrders) {
                System.out.println("Order " + m.get("id") + ", amount class: " + (m.get("amount") != null ? m.get("amount").getClass() : "null") + ", val: " + m.get("amount"));
                Object amountObj = m.get("amount");
                double val = ((Number)amountObj).doubleValue();
                System.out.println("  -> Double: " + val);
            }
            
            double totalDebt = unpaidOrders.stream()
                .mapToDouble(m -> ((Number)m.get("amount")).doubleValue())
                .sum();
                
            double remaining = hanMucNo - totalDebt;
            System.out.println("Total debt: " + totalDebt);
            System.out.println("Remaining: " + remaining);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
