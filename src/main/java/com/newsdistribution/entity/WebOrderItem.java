package com.newsdistribution.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity 
@Table(name = "web_order_items")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class WebOrderItem {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "order_id")
    private Integer orderId;
    
    @Column(name = "ma_bao")
    private String maBao;
    
    @Column(name = "ten_bao")
    private String tenBao;
    
    @Column(name = "don_gia")
    private BigDecimal donGia;
    
    @Column(name = "so_luong")
    private Integer soLuong;
    
    @Column(name = "thanh_tien")
    private BigDecimal thanhTien;
}
