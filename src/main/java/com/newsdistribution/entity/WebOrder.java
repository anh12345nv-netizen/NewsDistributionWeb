package com.newsdistribution.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.List;

@Entity 
@Table(name = "web_orders")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class WebOrder {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "order_code")
    private String orderCode;
    
    @Column(name = "user_id")
    private Integer userId;
    
    private String makh;
    
    @Column(name = "tu_ngay")
    private LocalDate tuNgay;
    
    @Column(name = "den_ngay")
    private LocalDate denNgay;
    
    @Column(name = "ghi_chu")
    private String ghiChu;
    
    @Column(name = "sync_status")
    private String syncStatus;    // PENDING | SYNCED | FAILED
    
    @Column(name = "winform_hd_code")
    private String winformHdCode;
    
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private List<WebOrderItem> items;
}
