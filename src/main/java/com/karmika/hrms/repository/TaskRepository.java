package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAssignedToOrderByDueDateAsc(Employee assignedTo);

    List<Task> findByAssignedByOrderByCreatedAtDesc(Employee assignedBy);

    List<Task> findByAssignedToAndStatusOrderByDueDateAsc(Employee assignedTo, Task.TaskStatus status);

    List<Task> findByStatusOrderByDueDateAsc(Task.TaskStatus status);
}
