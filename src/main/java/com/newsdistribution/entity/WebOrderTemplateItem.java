package com.newsdistribution.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity 
@Table(name = "web_order_template_items")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class WebOrderTemplateItem {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "template_id")
    private Integer templateId;
    
    @Column(name = "ma_bao")
    private String maBao;
    
    @Column(name = "so_luong")
    private Integer soLuong;
}
