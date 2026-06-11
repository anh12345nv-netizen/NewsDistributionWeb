package com.newsdistribution.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity 
@Table(name = "web_users")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class WebUser {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String username;
    
    @Column(name = "password_hash")
    private String passwordHash;
    
    private String role;       // AGENCY | ADMIN_WEB
    private String makh;       // maps to tabKHACHHANG.MAKH
    
    @Column(name = "ten_hien_thi")
    private String tenHienThi;
    
    private String email;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "ten_doanh_nghiep")
    private String tenDoanhNghiep;
    
    @Column(name = "ma_so_thue")
    private String maSoThue;
    
    @Column(name = "dia_chi")
    private String diaChi;
    
    @Column(name = "so_dien_thoai")
    private String soDienThoai;
    
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
