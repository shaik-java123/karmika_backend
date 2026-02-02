package com.karmika.hrms.controller;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.Notification;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final EmployeeRepository employeeRepository;

    /**
     * Get all notifications for the logged-in employee
     * GET /api/notifications
     */
    @GetMapping
    public ResponseEntity<?> getMyNotifications(Authentication auth) {
        try {
            String username = auth.getName();
            Employee employee = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            List<Notification> notifications = notificationService.getEmployeeNotifications(employee);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notifications", notifications));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get unread notifications count
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication auth) {
        try {
            String username = auth.getName();
            Employee employee = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            Long count = notificationService.getUnreadCount(employee);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Mark a notification as read
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            notificationService.markAsRead(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notification marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Mark all notifications as read
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication auth) {
        try {
            String username = auth.getName();
            Employee employee = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            notificationService.markAllAsRead(employee);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "All notifications marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Delete a notification
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteNotification(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notification deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Create a company-wide announcement (ADMIN & HR only)
     * POST /api/notifications/announce
     */
    @PostMapping("/announce")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> createAnnouncement(@RequestBody Map<String, String> data) {
        try {
            String title = data.get("title");
            String message = data.get("message");

            if (title == null || title.isEmpty() || message == null || message.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Title and message are required"));
            }

            // Get all active employees
            List<Employee> recipients = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getStatus() == Employee.EmployeeStatus.ACTIVE)
                    .toList();

            notificationService.createAnnouncement(recipients, title, message);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Announcement sent to " + recipients.size() + " employees"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }
}
