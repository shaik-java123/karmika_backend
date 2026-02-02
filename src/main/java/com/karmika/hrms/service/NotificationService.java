package com.karmika.hrms.service;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.Notification;
import com.karmika.hrms.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Create a notification for an employee
     */
    @Transactional
    public Notification createNotification(
            Employee employee,
            String title,
            String message,
            Notification.NotificationType type,
            Long relatedEntityId,
            String relatedEntityType) {
        Notification notification = new Notification();
        notification.setEmployee(employee);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setIsRead(false);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRelatedEntityType(relatedEntityType);

        return notificationRepository.save(notification);
    }

    /**
     * Create notification for leave approval
     */
    public void notifyLeaveApproval(Employee employee, String leaveType, Long leaveId) {
        String title = "Leave Approved";
        String message = String.format("Your %s request has been approved!",
                leaveType.replace("_", " "));

        createNotification(employee, title, message,
                Notification.NotificationType.LEAVE_APPROVED,
                leaveId, "LEAVE");
    }

    /**
     * Create notification for leave rejection
     */
    public void notifyLeaveRejection(Employee employee, String leaveType, Long leaveId, String reason) {
        String title = "Leave Rejected";
        String message = String.format("Your %s request has been rejected. %s",
                leaveType.replace("_", " "),
                reason != null && !reason.isEmpty() ? "Reason: " + reason : "");

        createNotification(employee, title, message,
                Notification.NotificationType.LEAVE_REJECTED,
                leaveId, "LEAVE");
    }

    /**
     * Notify manager about new leave application
     */
    public void notifyLeaveApplication(Employee manager, Employee applicant, String leaveType, Long leaveId) {
        String title = "New Leave Application";
        String message = String.format("%s %s has applied for %s.",
                applicant.getFirstName(), applicant.getLastName(),
                leaveType.replace("_", " "));

        createNotification(manager, title, message,
                Notification.NotificationType.LEAVE_APPLIED,
                leaveId, "LEAVE");
    }

    /**
     * Notify employee about new task assignment
     */
    public void notifyTaskAssignment(Employee employee, Employee assigner, String taskTitle, Long taskId) {
        String title = "New Task Assigned";
        String message = String.format("%s assigned you a new task: %s",
                assigner.getFirstName(), taskTitle);

        createNotification(employee, title, message,
                Notification.NotificationType.TASK_ASSIGNED,
                taskId, "TASK");
    }

    /**
     * Notify manager about task completion
     */
    public void notifyTaskCompletion(Employee manager, Employee assignee, String taskTitle, Long taskId) {
        String title = "Task Completed";
        String message = String.format("%s completed the task: %s",
                assignee.getFirstName(), taskTitle);

        createNotification(manager, title, message,
                Notification.NotificationType.TASK_COMPLETED,
                taskId, "TASK");
    }

    /**
     * Broadcast company announcement
     */
    @Transactional
    public void createAnnouncement(List<Employee> recipients, String title, String message) {
        for (Employee employee : recipients) {
            createNotification(employee, title, message,
                    Notification.NotificationType.ANNOUNCEMENT,
                    null, "ANNOUNCEMENT");
        }
    }

    /**
     * Get all notifications for an employee
     */
    public List<Notification> getEmployeeNotifications(Employee employee) {
        return notificationRepository.findByEmployeeOrderByCreatedAtDesc(employee);
    }

    /**
     * Get unread notifications for an employee
     */
    public List<Notification> getUnreadNotifications(Employee employee) {
        return notificationRepository.findByEmployeeAndIsReadOrderByCreatedAtDesc(employee, false);
    }

    /**
     * Get unread notification count
     */
    public Long getUnreadCount(Employee employee) {
        return notificationRepository.countByEmployeeAndIsRead(employee, false);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }

    /**
     * Mark all notifications as read for an employee
     */
    @Transactional
    public void markAllAsRead(Employee employee) {
        List<Notification> unreadNotifications = getUnreadNotifications(employee);
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * Delete a notification
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}
