package com.karmika.hrms.controller;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.Task;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.repository.TaskRepository;
import com.karmika.hrms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    /**
     * Create/Assign a task (MANAGER, ADMIN, HR)
     * POST /api/tasks/create
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<?> createTask(@RequestBody Map<String, Object> taskData, Authentication auth) {
        try {
            String username = auth.getName();
            Employee assignedBy = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "Your user account (" + username + ") is not linked to an employee record. " +
                                    "Please contact HR to create your employee profile."));

            Long assignedToId = Long.valueOf(taskData.get("assignedToId").toString());
            Employee assignedTo = employeeRepository.findById(assignedToId)
                    .orElseThrow(() -> new RuntimeException("Selected employee not found with ID: " + assignedToId));

            // Managers can only assign to their team members
            if (assignedBy.getUser().getRole().name().equals("MANAGER")) {
                if (assignedTo.getReportingManager() == null ||
                        !assignedTo.getReportingManager().getId().equals(assignedBy.getId())) {
                    return ResponseEntity.status(403).body(Map.of(
                            "success", false,
                            "error", "You can only assign tasks to your team members"));
                }
            }

            Task task = new Task();
            task.setTitle((String) taskData.get("title"));
            task.setDescription((String) taskData.get("description"));
            task.setAssignedTo(assignedTo);
            task.setAssignedBy(assignedBy);
            task.setDueDate(LocalDate.parse((String) taskData.get("dueDate")));
            task.setPriority(Task.TaskPriority.valueOf((String) taskData.get("priority")));
            task.setStatus(Task.TaskStatus.TODO);

            Task savedTask = taskRepository.save(task);

            // Send notification to assigned employee
            notificationService.notifyTaskAssignment(
                    assignedTo,
                    assignedBy,
                    task.getTitle(),
                    savedTask.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Task created successfully",
                    "task", savedTask));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get my tasks (All authenticated users)
     * GET /api/tasks/my-tasks
     */
    @GetMapping("/my-tasks")
    public ResponseEntity<?> getMyTasks(Authentication auth) {
        try {
            String username = auth.getName();
            Employee employee = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            List<Task> tasks = taskRepository.findByAssignedToOrderByDueDateAsc(employee);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "tasks", tasks));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get tasks assigned by me (MANAGER, ADMIN, HR)
     * GET /api/tasks/assigned-by-me
     */
    @GetMapping("/assigned-by-me")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<?> getTasksAssignedByMe(Authentication auth) {
        try {
            String username = auth.getName();
            Employee employee = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            List<Task> tasks = taskRepository.findByAssignedByOrderByCreatedAtDesc(employee);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "tasks", tasks));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get all tasks (ADMIN, HR)
     * GET /api/tasks/all
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> getAllTasks() {
        try {
            List<Task> tasks = taskRepository.findAll();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "tasks", tasks));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Update task status
     * PUT /api/tasks/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateTaskStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> data,
            Authentication auth) {
        try {
            String username = auth.getName();
            Employee employee = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            return taskRepository.findById(id)
                    .map(task -> {
                        // Only assigned employee or task creator can update status
                        if (!task.getAssignedTo().getId().equals(employee.getId()) &&
                                !task.getAssignedBy().getId().equals(employee.getId())) {
                            return ResponseEntity.status(403).body(Map.of(
                                    "success", false,
                                    "error", "You can only update your own tasks"));
                        }

                        Task.TaskStatus newStatus = Task.TaskStatus.valueOf(data.get("status"));
                        task.setStatus(newStatus);

                        if (newStatus == Task.TaskStatus.COMPLETED) {
                            task.setCompletedAt(LocalDateTime.now());

                            // Notify task assigner
                            notificationService.notifyTaskCompletion(
                                    task.getAssignedBy(),
                                    task.getAssignedTo(),
                                    task.getTitle(),
                                    task.getId());
                        }

                        if (data.containsKey("comments")) {
                            task.setComments(data.get("comments"));
                        }

                        taskRepository.save(task);

                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Task status updated successfully",
                                "task", task));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Update task details (MANAGER, ADMIN, HR)
     * PUT /api/tasks/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<?> updateTask(
            @PathVariable Long id,
            @RequestBody Map<String, Object> taskData,
            Authentication auth) {
        try {
            String username = auth.getName();
            Employee employee = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            return taskRepository.findById(id)
                    .map(task -> {
                        // Only task creator can update task details
                        if (!task.getAssignedBy().getId().equals(employee.getId())) {
                            return ResponseEntity.status(403).body(Map.of(
                                    "success", false,
                                    "error", "You can only update tasks you created"));
                        }

                        if (taskData.containsKey("title")) {
                            task.setTitle((String) taskData.get("title"));
                        }
                        if (taskData.containsKey("description")) {
                            task.setDescription((String) taskData.get("description"));
                        }
                        if (taskData.containsKey("dueDate")) {
                            task.setDueDate(LocalDate.parse((String) taskData.get("dueDate")));
                        }
                        if (taskData.containsKey("priority")) {
                            task.setPriority(Task.TaskPriority.valueOf((String) taskData.get("priority")));
                        }
                        // Allow re-assigning to a different employee
                        if (taskData.containsKey("assignedToId")) {
                            Long newAssigneeId = Long.valueOf(taskData.get("assignedToId").toString());
                            Employee newAssignee = employeeRepository.findById(newAssigneeId)
                                    .orElseThrow(() -> new RuntimeException("Target employee not found"));
                            task.setAssignedTo(newAssignee);
                            task.setStatus(Task.TaskStatus.TODO); // reset status on reassign
                            task.setCompletedAt(null);
                        }

                        taskRepository.save(task);

                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Task updated successfully",
                                "task", task));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Reassign a task to a different employee (task creator only)
     * POST /api/tasks/{id}/reassign
     * Body: { assignedToId, reason, resetStatus }
     */
    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<?> reassignTask(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            String username = auth.getName();
            Employee requester = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            Task task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            // Only the original assigner can reassign
            if (!task.getAssignedBy().getId().equals(requester.getId())) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "Only the task creator can reassign this task"));
            }

            Long newAssigneeId = Long.valueOf(body.get("assignedToId").toString());
            Employee newAssignee = employeeRepository.findById(newAssigneeId)
                    .orElseThrow(() -> new RuntimeException("Target employee not found with ID: " + newAssigneeId));

            String reason = body.containsKey("reason") ? (String) body.get("reason") : null;

            // Update task
            Employee previousAssignee = task.getAssignedTo();
            task.setAssignedTo(newAssignee);
            task.setStatus(Task.TaskStatus.TODO); // always reset to TODO on reassign
            task.setCompletedAt(null);
            if (reason != null && !reason.isBlank()) {
                task.setComments("Reassigned: " + reason);
            }

            taskRepository.save(task);

            // Notify new assignee
            notificationService.notifyTaskAssignment(
                    newAssignee,
                    requester,
                    task.getTitle(),
                    task.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", String.format("Task reassigned from %s %s to %s %s",
                            previousAssignee.getFirstName(), previousAssignee.getLastName(),
                            newAssignee.getFirstName(), newAssignee.getLastName()),
                    "task", task));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Delete a task (Only task creator)
     * DELETE /api/tasks/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, Authentication auth) {
        try {
            String username = auth.getName();
            Employee employee = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            return taskRepository.findById(id)
                    .map(task -> {
                        if (!task.getAssignedBy().getId().equals(employee.getId())) {
                            return ResponseEntity.status(403).body(Map.of(
                                    "success", false,
                                    "error", "You can only delete tasks you created"));
                        }

                        taskRepository.delete(task);

                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Task deleted successfully"));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }
}
