package com.newsdistribution.repository;

import com.newsdistribution.entity.WebOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebOrderItemRepository extends JpaRepository<WebOrderItem, Integer> {
}
