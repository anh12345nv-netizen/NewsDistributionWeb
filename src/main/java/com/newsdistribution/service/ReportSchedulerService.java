package com.newsdistribution.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class ReportSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(ReportSchedulerService.class);
    private final JdbcTemplate jdbcB;
    
    public ReportSchedulerService(@Qualifier("jdbcB") JdbcTemplate jdbcB) {
        this.jdbcB = jdbcB;
    }

    // Chạy vào ngày 1 hàng tháng lúc 00:00 (cron: 0 0 0 1 * ?)
    // Để test nhanh: chạy mỗi phút (cron: 0 * * * * ?) -> Đã bỏ comment để test thì đổi lại.
    @Scheduled(cron = "0 0 0 1 * ?")
    public void generateMonthlyReport() {
        logger.info("Generating monthly debt and sales reports for all agencies...");
        
        // Giả lập luồng tạo PDF và gửi Email
        java.util.List<String> agencies = jdbcB.queryForList("SELECT makh FROM web_users", String.class);
        for (String makh : agencies) {
            logger.info("Generating report PDF for agency: {}", makh);
            // ... Tạo file PDF/Excel ở đây bằng iText/Apache POI
            logger.info("Emailing report to agency: {}", makh);
        }
        
        logger.info("Monthly reports generated successfully.");
    }
}
