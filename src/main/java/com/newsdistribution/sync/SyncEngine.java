package com.newsdistribution.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@Slf4j
public class SyncEngine {

    private final JdbcTemplate jdbcA;
    private final JdbcTemplate jdbcC;
    private final SimpMessagingTemplate ws;

    public SyncEngine(@Qualifier("jdbcA") JdbcTemplate jdbcA,
                      @Qualifier("jdbcC") JdbcTemplate jdbcC,
                      SimpMessagingTemplate ws) {
        this.jdbcA = jdbcA;
        this.jdbcC = jdbcC;
        this.ws = ws;
    }

    @Scheduled(fixedDelayString = "${sync.interval-ms}")
    public void runSync() {
        log.info("Starting synchronization...");
        syncBao();
        syncKhachHang();
        syncHoaDon();
        syncTonKho();
        ws.convertAndSend("/topic/sync", Map.of("status", "completed", "at", new Date().toString()));
        log.info("Synchronization completed.");
    }

    private void syncBao() {
        try {
            var rows = jdbcA.queryForList(
                "SELECT maBao,ten,DVT,donGia,ngayBatDau,thu1,thu2,thu3,thu4,thu5,thu6,thu7,soLanPHtrongTuan,sogoc FROM tabBAO"
            );
            for (var r : rows) {
                jdbcC.update("""
                    MERGE m_bao AS t USING (SELECT ? AS maBao) AS s ON t.maBao=s.maBao
                    WHEN MATCHED THEN UPDATE SET ten=?,DVT=?,donGia=?,ngayBatDau=?,
                      thu1=?,thu2=?,thu3=?,thu4=?,thu5=?,thu6=?,thu7=?,
                      soLanPHtrongTuan=?,sogoc=?,synced_at=GETDATE()
                    WHEN NOT MATCHED THEN INSERT (maBao,ten,DVT,donGia,ngayBatDau,
                      thu1,thu2,thu3,thu4,thu5,thu6,thu7,soLanPHtrongTuan,sogoc)
                      VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?);
                    """,
                    r.get("maBao"),
                    r.get("ten"),r.get("DVT"),r.get("donGia"),r.get("ngayBatDau"),
                    r.get("thu1"),r.get("thu2"),r.get("thu3"),r.get("thu4"),r.get("thu5"),r.get("thu6"),r.get("thu7"),
                    r.get("soLanPHtrongTuan"),r.get("sogoc"),
                    r.get("maBao"),r.get("ten"),r.get("DVT"),r.get("donGia"),r.get("ngayBatDau"),
                    r.get("thu1"),r.get("thu2"),r.get("thu3"),r.get("thu4"),r.get("thu5"),r.get("thu6"),r.get("thu7"),
                    r.get("soLanPHtrongTuan"),r.get("sogoc")
                );
            }
            logSync("WINFORM_TO_MASTER","m_bao",rows.size(),"SUCCESS",null);
        } catch (Exception e) {
            log.error("syncBao failed: {}", e.getMessage());
            logSync("WINFORM_TO_MASTER","m_bao",0,"ERROR",e.getMessage());
        }
    }

    private void syncKhachHang() {
        try {
            var rows = jdbcA.queryForList(
                "SELECT MAKH,TEN,DIACHI,DIENTHOAI,CHIETKHAU,P_PH,P_KT,UUTIEN FROM tabKHACHHANG"
            );
            for (var r : rows) {
                jdbcC.update("""
                    MERGE m_khach_hang AS t USING (SELECT ? AS MAKH) AS s ON t.MAKH=s.MAKH
                    WHEN MATCHED THEN UPDATE SET TEN=?,DIACHI=?,DIENTHOAI=?,CHIETKHAU=?,P_PH=?,P_KT=?,UUTIEN=?,synced_at=GETDATE()
                    WHEN NOT MATCHED THEN INSERT (MAKH,TEN,DIACHI,DIENTHOAI,CHIETKHAU,P_PH,P_KT,UUTIEN)
                      VALUES (?,?,?,?,?,?,?,?);
                    """,
                    r.get("MAKH"),
                    r.get("TEN"),r.get("DIACHI"),r.get("DIENTHOAI"),r.get("CHIETKHAU"),r.get("P_PH"),r.get("P_KT"),r.get("UUTIEN"),
                    r.get("MAKH"),r.get("TEN"),r.get("DIACHI"),r.get("DIENTHOAI"),r.get("CHIETKHAU"),r.get("P_PH"),r.get("P_KT"),r.get("UUTIEN")
                );
            }
            logSync("WINFORM_TO_MASTER","m_khach_hang",rows.size(),"SUCCESS",null);
        } catch (Exception e) {
            log.error("syncKhachHang failed: {}", e.getMessage());
            logSync("WINFORM_TO_MASTER","m_khach_hang",0,"ERROR",e.getMessage());
        }
    }

    private void syncHoaDon() {
        try {
            var rows = jdbcA.queryForList("""
                SELECT h.sohd, h.makh, k.TEN as ten_kh,
                       h.ngayLapPhieu, h.tuNgay, h.denNgay, h.ghichu, h.thanhToan,
                       ISNULL(SUM(c.thanhTien),0) as tong_tien,
                       ISNULL(SUM(c.soLuongThuc+c.soLuongPhatSinh),0) as tong_so_bao
                FROM tabHOADON h
                LEFT JOIN tabKHACHHANG k ON h.makh=k.MAKH
                LEFT JOIN tabCHITIETHOADON c ON h.sohd=c.sohd
                GROUP BY h.sohd,h.makh,k.TEN,h.ngayLapPhieu,h.tuNgay,h.denNgay,h.ghichu,h.thanhToan
                """);
            for (var r : rows) {
                jdbcC.update("""
                    MERGE m_hoa_don AS t USING (SELECT ? AS sohd, 'WINFORM' AS source) AS s
                      ON t.sohd=s.sohd AND t.source=s.source
                    WHEN MATCHED THEN UPDATE SET makh=?,ten_kh=?,ngayLapPhieu=?,tuNgay=?,denNgay=?,
                      ghichu=?,thanhToan=?,tong_tien=?,tong_so_bao=?,synced_at=GETDATE()
                    WHEN NOT MATCHED THEN INSERT (sohd,makh,ten_kh,ngayLapPhieu,tuNgay,denNgay,ghichu,thanhToan,tong_tien,tong_so_bao,source)
                      VALUES (?,?,?,?,?,?,?,?,?,?,'WINFORM');
                    """,
                    r.get("sohd"),
                    r.get("makh"),r.get("ten_kh"),r.get("ngayLapPhieu"),r.get("tuNgay"),r.get("denNgay"),
                    r.get("ghichu"),r.get("thanhToan"),r.get("tong_tien"),r.get("tong_so_bao"),
                    r.get("sohd"),r.get("makh"),r.get("ten_kh"),r.get("ngayLapPhieu"),r.get("tuNgay"),r.get("denNgay"),
                    r.get("ghichu"),r.get("thanhToan"),r.get("tong_tien"),r.get("tong_so_bao")
                );
            }
            logSync("WINFORM_TO_MASTER","m_hoa_don",rows.size(),"SUCCESS",null);
        } catch (Exception e) {
            log.error("syncHoaDon failed: {}", e.getMessage());
            logSync("WINFORM_TO_MASTER","m_hoa_don",0,"ERROR",e.getMessage());
        }
    }

    private void syncTonKho() {
        try {
            var rows = jdbcA.queryForList(
                "SELECT ngay,maBao,tenBao,soBao,slPhatHanh,banthuc,banLe,dieuPhoi,ton FROM tabTon"
            );
            // tabTon không có PK rõ ràng → xóa và insert lại 7 ngày gần nhất
            jdbcC.update("DELETE FROM m_ton_kho WHERE ngay >= DATEADD(day,-7,GETDATE())");
            for (var r : rows) {
                jdbcC.update("""
                    INSERT INTO m_ton_kho (ngay,maBao,tenBao,slPhatHanh,banthuc,banLe,dieuPhoi,ton)
                    VALUES (?,?,?,?,?,?,?,?)
                    """,
                    r.get("ngay"),r.get("maBao"),r.get("tenBao"),
                    r.get("slPhatHanh"),r.get("banthuc"),r.get("banLe"),r.get("dieuPhoi"),r.get("ton")
                );
            }
            logSync("WINFORM_TO_MASTER","m_ton_kho",rows.size(),"SUCCESS",null);
        } catch (Exception e) {
            log.error("syncTonKho failed: {}", e.getMessage());
            logSync("WINFORM_TO_MASTER","m_ton_kho",0,"ERROR",e.getMessage());
        }
    }

    private void logSync(String type, String table, int rows, String status, String err) {
        try {
            jdbcC.update(
                "INSERT INTO sync_log (sync_type,table_name,rows_synced,status,error_msg) VALUES (?,?,?,?,?)",
                type, table, rows, status, err
            );
        } catch (Exception ignored) {}
    }
}
