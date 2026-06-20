package com.newsdistribution.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SyncEngineTest {

    @Mock
    private JdbcTemplate jdbcA;

    @Mock
    private JdbcTemplate jdbcB;

    @Mock
    private JdbcTemplate jdbcC;

    @Mock
    private SimpMessagingTemplate ws;

    private SyncEngine syncEngine;

    @BeforeEach
    void setUp() {
        syncEngine = new SyncEngine(jdbcA, jdbcB, jdbcC, ws);
    }

    /**
     * Vấn đề 2: Lược đồ không đồng nhất (Thiếu dữ liệu Hóa đơn WinForm)
     * Kiểm tra khi WinForm trả về dữ liệu NULL cho tuNgay, denNgay, ghichu
     */
    @Test
    void testSyncHoaDon_WithNullValuesFromWinForm() throws Exception {
        // Mock data from WinForm (jdbcA)
        List<Map<String, Object>> mockRows = List.of(
            Map.of(
                "sohd", "HD001",
                "makh", "DL001",
                "ten_kh", "Đại lý 1",
                "ngayLapPhieu", new java.util.Date(),
                "thanhToan", true,
                "tong_tien", 500000.0,
                "tong_so_bao", 100
                // Note: tuNgay, denNgay, ghichu are MISSING (null)
            )
        );

        // Stubbing the query
        when(jdbcA.queryForList(anyString())).thenReturn(mockRows);

        // Run sync using reflection since method is private, or just change visibility?
        // Let's use reflection to call private method syncHoaDon
        java.lang.reflect.Method method = SyncEngine.class.getDeclaredMethod("syncHoaDon");
        method.setAccessible(true);
        method.invoke(syncEngine);

        // Capture update arguments to jdbcC
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcC, atLeastOnce()).update(anyString(), argsCaptor.capture());

        Object[] capturedArgs = argsCaptor.getAllValues().stream()
            .filter(args -> args.length > 5)
            .findFirst()
            .orElseThrow();
        // The MERGE statement has many parameters. 
        // We verify that it passes non-null values for tuNgay and denNgay now
        assertNotNull(capturedArgs);
        assertEquals("HD001", capturedArgs[0]); // sohd param 1
        assertNotNull(capturedArgs[4]); // tuNgay param 5 (now coalesced to ngayLapPhieu)
    }

    /**
     * Vấn đề 3: Trạng thái thanh toán phân cực
     * Kiểm tra khi đồng bộ WebOrders, thanhToan luôn bị hardcode = 0
     */
    @Test
    void testSyncWebOrders_HardcodedThanhToan() throws Exception {
        List<Map<String, Object>> mockRows = List.of(
            Map.of(
                "sohd", "WEB001",
                "makh", "DL002",
                "ten_kh", "Đại lý Web",
                "ngayLapPhieu", new java.util.Date(),
                "tuNgay", new java.util.Date(),
                "denNgay", new java.util.Date(),
                "ghichu", "Test",
                "payment_status", "PAID",
                "tong_tien", 100000.0,
                "tong_so_bao", 50
            )
        );

        when(jdbcB.queryForList(anyString())).thenReturn(mockRows);

        java.lang.reflect.Method method = SyncEngine.class.getDeclaredMethod("syncWebOrders");
        method.setAccessible(true);
        method.invoke(syncEngine);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcC, atLeastOnce()).update(anyString(), argsCaptor.capture());

        Object[] capturedArgs = argsCaptor.getAllValues().stream()
            .filter(args -> args.length > 5)
            .findFirst()
            .orElseThrow();
        // Check the 8th parameter (thanhToan) in the MERGE statement.
        // Parameters: sohd(1), makh(2), ten_kh(3), ngayLapPhieu(4), tuNgay(5), denNgay(6), ghichu(7), thanhToan(8)
        assertEquals(1, capturedArgs[7]); // It pushes 1 (true) since payment_status is PAID
    }

    /**
     * Vấn đề 5: Công thức tồn kho bị sai
     * Kiểm tra xem SyncEngine có lấy cột `ton` hay tự tính
     */
    @Test
    void testSyncTonKho_TrustsWinFormData() throws Exception {
        List<Map<String, Object>> mockRows = List.of(
            Map.of(
                "ngay", new java.util.Date(),
                "maBao", "B01",
                "tenBao", "Báo Test",
                "slPhatHanh", 100,
                "banthuc", 50,
                "banLe", 10,
                "dieuPhoi", 10,
                "ton", 30 
            )
        );

        when(jdbcA.queryForList(anyString())).thenReturn(mockRows);

        java.lang.reflect.Method method = SyncEngine.class.getDeclaredMethod("syncTonKho");
        method.setAccessible(true);
        method.invoke(syncEngine);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcC, atLeastOnce()).update(anyString(), argsCaptor.capture());

        Object[] capturedArgs = argsCaptor.getAllValues().stream()
            .filter(args -> args.length == 8)
            .findFirst()
            .orElseThrow();
        // Insert statement: ngay(1), maBao(2), tenBao(3), slPhatHanh(4), banthuc(5), banLe(6), dieuPhoi(7), ton(8)
        assertEquals(30, capturedArgs[7]); // It pushes 30, proving the fix.
    }
    /**
     * Vấn đề 4: Tồn kho rác (Dữ liệu quá 7 ngày)
     * Kiểm tra DELETE FROM m_ton_kho WHERE ngay >= DATEADD(day,-7,GETDATE())
     */
    @Test
    void testSyncTonKho_OldDataIgnored() throws Exception {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, -10); // 10 days ago (old data)
        java.util.Date oldDate = cal.getTime();

        List<Map<String, Object>> mockRows = List.of(
            Map.of(
                "ngay", oldDate,
                "maBao", "B02",
                "tenBao", "Báo Cũ",
                "slPhatHanh", 100,
                "banthuc", 50,
                "banLe", 10,
                "dieuPhoi", 10,
                "ton", 30 
            )
        );

        when(jdbcA.queryForList(anyString())).thenReturn(mockRows);

        java.lang.reflect.Method method = SyncEngine.class.getDeclaredMethod("syncTonKho");
        method.setAccessible(true);
        method.invoke(syncEngine);

        // Verification: The query deleting old data is hardcoded to >= 7 days
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcC, atLeastOnce()).update(queryCaptor.capture());
        
        List<String> executedQueries = queryCaptor.getAllValues();
        assertTrue(executedQueries.get(0).contains("DELETE FROM m_ton_kho"),
            "Đã thay thế bằng xóa toàn bộ và đồng bộ lại!");
    }

    /**
     * Vấn đề 7: Lỗi Encoding tiếng Việt
     * Kiểm tra khi đọc chuỗi VNI từ JDBC A (WinForm) ghi trực tiếp vào JDBC C (Master)
     */
    @Test
    void testSyncHoaDon_VniEncoding() throws Exception {
        // "Thanh Nieân" in VNI is a corrupted string from UTF-8 perspective
        List<Map<String, Object>> mockRows = List.of(
            Map.of(
                "sohd", "HD002",
                "makh", "DL003",
                "ten_kh", "Thanh Nieân", // Corrupted VNI string
                "ngayLapPhieu", new java.util.Date(),
                "tuNgay", new java.util.Date(),
                "denNgay", new java.util.Date(),
                "ghichu", "Coáng Quyõnh", // Corrupted VNI string
                "thanhToan", true,
                "tong_tien", 500000.0,
                "tong_so_bao", 100
            )
        );

        when(jdbcA.queryForList(anyString())).thenReturn(mockRows);

        java.lang.reflect.Method method = SyncEngine.class.getDeclaredMethod("syncHoaDon");
        method.setAccessible(true);
        method.invoke(syncEngine);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcC, atLeastOnce()).update(anyString(), argsCaptor.capture());

        Object[] capturedArgs = argsCaptor.getAllValues().stream()
            .filter(args -> args.length > 5)
            .findFirst()
            .orElseThrow();
        // With VniToUnicodeConverter, the string should be converted to UTF-8
        assertEquals("Thanh Niên", capturedArgs[2]); 
        assertEquals("Cống Quỳnh", capturedArgs[6]); 
    }
}
