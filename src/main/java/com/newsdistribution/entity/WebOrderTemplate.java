package com.newsdistribution.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity 
@Table(name = "web_order_templates")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class WebOrderTemplate {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String makh;
    
    @Column(name = "template_name")
    private String templateName;
    
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "template_id")
    private List<WebOrderTemplateItem> items;
}
