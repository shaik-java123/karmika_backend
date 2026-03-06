package com.karmika.hrms.repository;

import com.karmika.hrms.entity.InventoryRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRequestRepository extends JpaRepository<InventoryRequest, Long> {

    List<InventoryRequest> findByEmployeeIdOrderByRequestDateDesc(Long employeeId);

    List<InventoryRequest> findByStatusOrderByRequestDateAsc(InventoryRequest.RequestStatus status);
}
