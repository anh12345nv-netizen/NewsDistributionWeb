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
public class SyncEngineDeduplicationTest {

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
     * Vấn đề 6: Tổng hóa đơn phình to
     * Kiểm tra khi đồng bộ bị gọi 2 lần với cùng 1 hóa đơn, có xảy ra trùng lặp không?
     * Thực tế với lệnh MERGE ... ON sohd=s.sohd, nó xử lý khá tốt ở cấp độ CSDL.
     * Tuy nhiên, nếu query từ tabHOADON bị JOIN nhân đôi dòng, MERGE sẽ cập nhật hoặc insert nhiều lần.
     */
    @Test
    void testSyncHoaDon_DuplicatesFromJoin() throws Exception {
        // Giả sử do JOIN với tabCHITIETHOADON mà không GROUP BY kĩ, 1 hóa đơn bị nhân thành 2 dòng
        List<Map<String, Object>> mockRows = List.of(
            Map.of(
                "sohd", "HD005",
                "makh", "DL005",
                "ten_kh", "Đại lý 5",
                "ngayLapPhieu", new java.util.Date(),
                "tuNgay", new java.util.Date(),
                "denNgay", new java.util.Date(),
                "ghichu", "",
                "thanhToan", true,
                "tong_tien", 100000.0,
                "tong_so_bao", 50
            ),
            Map.of(
                "sohd", "HD005", // Same invoice!
                "makh", "DL005",
                "ten_kh", "Đại lý 5",
                "ngayLapPhieu", new java.util.Date(),
                "tuNgay", new java.util.Date(),
                "denNgay", new java.util.Date(),
                "ghichu", "",
                "thanhToan", true,
                "tong_tien", 200000.0, // Different detail values due to bad JOIN
                "tong_so_bao", 100
            )
        );

        when(jdbcA.queryForList(anyString())).thenReturn(mockRows);

        java.lang.reflect.Method method = SyncEngine.class.getDeclaredMethod("syncHoaDon");
        method.setAccessible(true);
        method.invoke(syncEngine);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcC, atLeastOnce()).update(anyString(), argsCaptor.capture());

        List<Object[]> allArgs = argsCaptor.getAllValues().stream()
            .filter(args -> args.length > 5)
            .collect(java.util.stream.Collectors.toList());
        assertEquals(2, allArgs.size());
        // Cùng 1 mã HD005 nhưng bị insert/update 2 lần dẫn đến ghi đè lung tung hoặc phình data
        assertEquals("HD005", allArgs.get(0)[0]);
        assertEquals("HD005", allArgs.get(1)[0]);
    }
}
