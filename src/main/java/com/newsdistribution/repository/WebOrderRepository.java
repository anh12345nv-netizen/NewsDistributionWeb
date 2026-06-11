package com.newsdistribution.repository;

import com.newsdistribution.entity.WebOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface WebOrderRepository extends JpaRepository<WebOrder, Integer> {
    List<WebOrder> findByMakhOrderByCreatedAtDesc(String makh);
    List<WebOrder> findBySyncStatus(String syncStatus);

    @Query("SELECT o FROM WebOrder o WHERE o.syncStatus = 'PENDING' ORDER BY o.createdAt ASC")
    List<WebOrder> findPendingOrders();
}
