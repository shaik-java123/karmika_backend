package com.karmika.hrms.repository;

import com.karmika.hrms.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByStatus(InventoryItem.ItemStatus status);

    List<InventoryItem> findByCategory(String category);
}
