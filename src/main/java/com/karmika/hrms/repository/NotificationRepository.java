package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByEmployeeOrderByCreatedAtDesc(Employee employee);

    List<Notification> findByEmployeeAndIsReadOrderByCreatedAtDesc(Employee employee, Boolean isRead);

    Long countByEmployeeAndIsRead(Employee employee, Boolean isRead);
}
