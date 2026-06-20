package com.newsdistribution.repository;

import com.newsdistribution.entity.WebOrderTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WebOrderTemplateRepository extends JpaRepository<WebOrderTemplate, Integer> {
    List<WebOrderTemplate> findByMakh(String makh);
}
