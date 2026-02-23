package com.karmika.hrms.service;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.Notification;
import com.karmika.hrms.entity.OnboardingChecklist;
import com.karmika.hrms.entity.OnboardingDocument;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.repository.OnboardingChecklistRepository;
import com.karmika.hrms.repository.OnboardingDocumentRepository;
import com.karmika.hrms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final OnboardingChecklistRepository checklistRepo;
    private final OnboardingDocumentRepository documentRepo;
    private final EmployeeRepository employeeRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private Employee getEmployee(Long id) {
        return employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
    }

    private Employee getEmployeeByUsername(String username) {
        var user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return employeeRepo.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Employee profile not found for: " + username));
    }

    /** Silently find ALL HR/Admin employees to notify them as a group */
    private List<Employee> getHrEmployees() {
        try {
            return employeeRepo.findAll().stream()
                    .filter(e -> {
                        var u = userRepo.findByEmail(e.getEmail());
                        return u.isPresent() && (u.get().getRole().name().equals("HR")
                                || u.get().getRole().name().equals("ADMIN"));
                    })
                    .toList();
        } catch (Exception ex) {
            log.warn("Could not fetch HR employees for notification: {}", ex.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────────────────
    // CHECKLIST — HR operations
    // ─────────────────────────────────────────────────────────

    /**
     * Start onboarding: seed default checklist, then notify the employee.
     */
    public List<Map<String, Object>> startOnboarding(Long employeeId, String hrUsername) {
        Employee employee = getEmployee(employeeId);
        Employee hr = getEmployeeByUsername(hrUsername);

        if (checklistRepo.countByEmployeeId(employeeId) == 0) {
            List<OnboardingChecklist> defaults = new ArrayList<>();
            defaults.add(createDefaultTask(employee, hr, "Upload Profile Photo",
                    "Upload a recent, clear passport-size photo with a white background.",
                    OnboardingChecklist.TaskType.UPLOAD_PHOTO, 3));
            defaults.add(createDefaultTask(employee, hr, "Upload Aadhaar Card",
                    "Upload a clear scan or photo of your Aadhaar card (front and back).",
                    OnboardingChecklist.TaskType.UPLOAD_DOCUMENT, 5));
            defaults.add(createDefaultTask(employee, hr, "Upload PAN Card",
                    "Upload a clear scan or photo of your PAN card.",
                    OnboardingChecklist.TaskType.UPLOAD_DOCUMENT, 5));
            defaults.add(createDefaultTask(employee, hr, "Upload Educational Certificates",
                    "Upload your highest educational qualification certificate.",
                    OnboardingChecklist.TaskType.UPLOAD_DOCUMENT, 7));
            defaults.add(createDefaultTask(employee, hr, "Upload Experience Letter",
                    "Upload experience/relieving letter from your previous employer (if applicable).",
                    OnboardingChecklist.TaskType.UPLOAD_DOCUMENT, 7));
            defaults.add(createDefaultTask(employee, hr, "Upload Bank Details",
                    "Upload a cancelled cheque or bank passbook page showing your account number and IFSC.",
                    OnboardingChecklist.TaskType.UPLOAD_DOCUMENT, 5));
            defaults.add(createDefaultTask(employee, hr, "Sign & Upload NDA",
                    "Download the NDA from HR Documents, sign it, and upload the signed copy.",
                    OnboardingChecklist.TaskType.SIGN_DOCUMENT, 7));
            defaults.add(createDefaultTask(employee, hr, "Acknowledge Company Handbook",
                    "Read the company handbook (shared in HR Documents) and acknowledge you have read it.",
                    OnboardingChecklist.TaskType.ACKNOWLEDGEMENT, 5));
            checklistRepo.saveAll(defaults);

            // Notify the employee that onboarding has started
            notificationService.createNotification(
                    employee,
                    "[Onboarding] Your Onboarding Has Started!",
                    String.format("Welcome, %s! %s has initiated your onboarding. " +
                            "You have %d tasks to complete. Visit the Onboarding page to get started.",
                            employee.getFirstName(), hr.getFirstName(), defaults.size()),
                    Notification.NotificationType.ONBOARDING_STARTED,
                    employeeId, "ONBOARDING");
        }

        return getOnboardingStatus(employeeId);
    }

    private OnboardingChecklist createDefaultTask(Employee employee, Employee hr,
            String title, String description, OnboardingChecklist.TaskType type, int dueDays) {
        OnboardingChecklist task = new OnboardingChecklist();
        task.setEmployee(employee);
        task.setAssignedBy(hr);
        task.setTitle(title);
        task.setDescription(description);
        task.setTaskType(type);
        task.setStatus(OnboardingChecklist.ChecklistStatus.PENDING);
        task.setDueDate(LocalDate.now().plusDays(dueDays));
        return task;
    }

    /** HR adds a custom checklist task and notifies the employee */
    public Map<String, Object> addChecklistTask(Long employeeId, String hrUsername, Map<String, Object> body) {
        Employee employee = getEmployee(employeeId);
        Employee hr = getEmployeeByUsername(hrUsername);

        OnboardingChecklist task = new OnboardingChecklist();
        task.setEmployee(employee);
        task.setAssignedBy(hr);
        task.setTitle((String) body.get("title"));
        task.setDescription((String) body.get("description"));
        task.setHrNotes((String) body.get("hrNotes"));

        String taskTypeStr = (String) body.getOrDefault("taskType", "GENERAL");
        task.setTaskType(OnboardingChecklist.TaskType.valueOf(taskTypeStr));

        String dueDateStr = (String) body.get("dueDate");
        if (dueDateStr != null && !dueDateStr.isBlank()) {
            task.setDueDate(LocalDate.parse(dueDateStr));
        }

        checklistRepo.save(task);

        // Notify employee about new task
        notificationService.createNotification(
                employee,
                "[Onboarding] New Task Assigned",
                String.format("%s assigned you a new onboarding task: \"%s\".",
                        hr.getFirstName() + " " + hr.getLastName(), task.getTitle()),
                Notification.NotificationType.TASK_ASSIGNED,
                task.getId(), "ONBOARDING");

        return toChecklistDto(task);
    }

    /**
     * HR reviews and approves/rejects/waives a submitted item, then notifies
     * employee
     */
    public Map<String, Object> reviewChecklistTask(Long taskId, String hrUsername, String action, String reason) {
        OnboardingChecklist task = checklistRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));
        Employee hr = getEmployeeByUsername(hrUsername);
        Employee employee = task.getEmployee();

        Notification.NotificationType notifType;
        String notifTitle, notifMsg;

        if ("approve".equalsIgnoreCase(action)) {
            task.setStatus(OnboardingChecklist.ChecklistStatus.APPROVED);
            task.setRejectionReason(null);
            notifType = Notification.NotificationType.ONBOARDING_TASK_APPROVED;
            notifTitle = "[Onboarding] Task Approved";
            notifMsg = String.format("%s approved your submission for \"%s\". Keep it up!",
                    hr.getFirstName(), task.getTitle());

        } else if ("reject".equalsIgnoreCase(action)) {
            task.setStatus(OnboardingChecklist.ChecklistStatus.REJECTED);
            task.setRejectionReason(reason);
            notifType = Notification.NotificationType.ONBOARDING_TASK_REJECTED;
            notifTitle = "[Onboarding] Task Rejected";
            notifMsg = String.format("%s rejected your submission for \"%s\". Reason: %s",
                    hr.getFirstName(), task.getTitle(),
                    reason != null ? reason : "No reason provided");

        } else if ("waive".equalsIgnoreCase(action)) {
            task.setStatus(OnboardingChecklist.ChecklistStatus.WAIVED);
            notifType = Notification.NotificationType.ONBOARDING_TASK_WAIVED;
            notifTitle = "[Onboarding] Task Waived";
            notifMsg = String.format("The task \"%s\" has been waived by %s.",
                    task.getTitle(), hr.getFirstName());

        } else {
            throw new RuntimeException("Unknown action: " + action);
        }

        checklistRepo.save(task);

        // 🔔 Notify employee about HR's decision
        notificationService.createNotification(employee, notifTitle, notifMsg,
                notifType, taskId, "ONBOARDING");

        // Check overall progress and notify HR with progress summary
        long total = checklistRepo.countByEmployeeId(employee.getId());
        long approved = checklistRepo.countByEmployeeIdAndStatus(employee.getId(),
                OnboardingChecklist.ChecklistStatus.APPROVED);
        long waived = checklistRepo.countByEmployeeIdAndStatus(employee.getId(),
                OnboardingChecklist.ChecklistStatus.WAIVED);
        long completed = approved + waived;
        int pct = total == 0 ? 0 : (int) Math.round((completed * 100.0) / total);

        // Notify HR about progress if it's a milestone
        if (pct == 100) {
            notificationService.createNotification(hr,
                    "[Onboarding] Complete!",
                    String.format("%s %s has completed all onboarding tasks (100%%).",
                            employee.getFirstName(), employee.getLastName()),
                    Notification.NotificationType.INFO, employee.getId(), "ONBOARDING");
        } else if (pct >= 50 && completed == total / 2) {
            notificationService.createNotification(hr,
                    "[Onboarding] Progress Update",
                    String.format("%s %s is 50%% through onboarding (%d/%d tasks done).",
                            employee.getFirstName(), employee.getLastName(), completed, total),
                    Notification.NotificationType.INFO, employee.getId(), "ONBOARDING");
        }

        return toChecklistDto(task);
    }

    /** Delete a checklist item (HR only) */
    public void deleteChecklistTask(Long taskId) {
        checklistRepo.deleteById(taskId);
    }

    // ─────────────────────────────────────────────────────────
    // CHECKLIST — Employee submit
    // ─────────────────────────────────────────────────────────

    /**
     * Employee submits a checklist item with optional file. Notifies all HR
     * employees.
     */
    public Map<String, Object> submitChecklistTask(Long taskId, String employeeUsername,
            String notes, MultipartFile file) throws IOException {

        OnboardingChecklist task = checklistRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        Employee emp = getEmployeeByUsername(employeeUsername);
        if (!task.getEmployee().getId().equals(emp.getId())) {
            throw new RuntimeException("Unauthorized: this task does not belong to you");
        }

        if (notes != null)
            task.setEmployeeNotes(notes);

        if (file != null && !file.isEmpty()) {
            String savedPath = saveFile(file, "checklist/" + taskId);
            task.setAttachmentPath(savedPath);
            task.setAttachmentName(file.getOriginalFilename());
        }

        task.setStatus(OnboardingChecklist.ChecklistStatus.IN_REVIEW);
        task.setCompletedAt(LocalDateTime.now());
        checklistRepo.save(task);

        // Notify ALL HR employees about the submission
        List<Employee> hrList = getHrEmployees();
        for (Employee hr : hrList) {
            notificationService.createNotification(
                    hr,
                    "[Onboarding] Task Submitted for Review",
                    String.format("%s %s submitted \"%s\" for review. Please review it in the Onboarding section.",
                            emp.getFirstName(), emp.getLastName(), task.getTitle()),
                    Notification.NotificationType.ONBOARDING_TASK_SUBMITTED,
                    taskId, "ONBOARDING");
        }

        return toChecklistDto(task);
    }

    // ─────────────────────────────────────────────────────────
    // DOCUMENTS — HR uploads
    // ─────────────────────────────────────────────────────────

    public Map<String, Object> uploadHrDocument(Long employeeId, String hrUsername,
            String documentName, String description, String documentType, MultipartFile file) throws IOException {

        Employee employee = getEmployee(employeeId);
        Employee hr = getEmployeeByUsername(hrUsername);

        String savedPath = saveFile(file, "onboarding/" + employeeId + "/hr");

        OnboardingDocument doc = new OnboardingDocument();
        doc.setEmployee(employee);
        doc.setUploadedBy(hr);
        doc.setSource(OnboardingDocument.DocumentSource.HR_UPLOAD);
        doc.setDocumentName(documentName);
        doc.setDescription(description);
        doc.setFilePath(savedPath);
        doc.setOriginalFileName(file.getOriginalFilename());
        doc.setMimeType(file.getContentType());
        doc.setFileSize(file.getSize());

        if (documentType != null && !documentType.isBlank()) {
            try {
                doc.setDocumentType(OnboardingDocument.DocumentType.valueOf(documentType));
            } catch (IllegalArgumentException ignored) {
                doc.setDocumentType(OnboardingDocument.DocumentType.OTHER);
            }
        }

        documentRepo.save(doc);

        // Notify the employee that HR shared a document
        notificationService.createNotification(
                employee,
                "[Onboarding] New Document Shared by HR",
                String.format("%s shared a document with you: \"%s\". Check the Onboarding > Documents tab.",
                        hr.getFirstName() + " " + hr.getLastName(), documentName),
                Notification.NotificationType.ONBOARDING_DOCUMENT_SHARED,
                doc.getId(), "ONBOARDING");

        return toDocumentDto(doc);
    }

    // ─────────────────────────────────────────────────────────
    // STATUS overview
    // ─────────────────────────────────────────────────────────

    public List<Map<String, Object>> getOnboardingStatus(Long employeeId) {
        List<OnboardingChecklist> tasks = checklistRepo.findByEmployeeIdOrderByCreatedAtAsc(employeeId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (OnboardingChecklist t : tasks)
            result.add(toChecklistDto(t));
        return result;
    }

    public List<Map<String, Object>> getDocuments(Long employeeId) {
        List<OnboardingDocument> docs = documentRepo.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (OnboardingDocument d : docs)
            result.add(toDocumentDto(d));
        return result;
    }

    public Map<String, Object> getOnboardingSummary(Long employeeId) {
        long total = checklistRepo.countByEmployeeId(employeeId);
        long approved = checklistRepo.countByEmployeeIdAndStatus(employeeId,
                OnboardingChecklist.ChecklistStatus.APPROVED);
        long inReview = checklistRepo.countByEmployeeIdAndStatus(employeeId,
                OnboardingChecklist.ChecklistStatus.IN_REVIEW);
        long rejected = checklistRepo.countByEmployeeIdAndStatus(employeeId,
                OnboardingChecklist.ChecklistStatus.REJECTED);
        long waived = checklistRepo.countByEmployeeIdAndStatus(employeeId,
                OnboardingChecklist.ChecklistStatus.WAIVED);
        long completed = approved + waived;

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", total);
        summary.put("approved", approved);
        summary.put("inReview", inReview);
        summary.put("rejected", rejected);
        summary.put("waived", waived);
        summary.put("pending", total - completed - inReview - rejected);
        summary.put("completionPercent", total == 0 ? 0 : Math.round((completed * 100.0) / total));
        summary.put("isOnboarding", total > 0);
        return summary;
    }

    // ─────────────────────────────────────────────────────────
    // FILE STORAGE
    // ─────────────────────────────────────────────────────────

    private String saveFile(MultipartFile file, String subDir) throws IOException {
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + ext;
        Path dir = Paths.get(uploadDir, subDir);
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        // URL-friendly path served by WebConfig static handler
        return "/uploads/" + subDir + "/" + filename;
    }

    // ─────────────────────────────────────────────────────────
    // DTO MAPPERS
    // ─────────────────────────────────────────────────────────

    private Map<String, Object> toChecklistDto(OnboardingChecklist t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("taskType", t.getTaskType());
        m.put("status", t.getStatus());
        m.put("dueDate", t.getDueDate());
        m.put("hrNotes", t.getHrNotes());
        m.put("employeeNotes", t.getEmployeeNotes());
        m.put("attachmentPath", t.getAttachmentPath());
        m.put("attachmentName", t.getAttachmentName());
        m.put("rejectionReason", t.getRejectionReason());
        m.put("completedAt", t.getCompletedAt());
        m.put("createdAt", t.getCreatedAt());
        m.put("employeeId", t.getEmployee().getId());
        if (t.getAssignedBy() != null) {
            m.put("assignedBy", t.getAssignedBy().getFirstName() + " " + t.getAssignedBy().getLastName());
        }
        return m;
    }

    private Map<String, Object> toDocumentDto(OnboardingDocument d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("documentName", d.getDocumentName());
        m.put("description", d.getDescription());
        m.put("documentType", d.getDocumentType());
        m.put("source", d.getSource());
        m.put("filePath", d.getFilePath());
        m.put("originalFileName", d.getOriginalFileName());
        m.put("mimeType", d.getMimeType());
        m.put("fileSize", d.getFileSize());
        m.put("createdAt", d.getCreatedAt());
        m.put("employeeId", d.getEmployee().getId());
        if (d.getUploadedBy() != null) {
            m.put("uploadedBy", d.getUploadedBy().getFirstName() + " " + d.getUploadedBy().getLastName());
        }
        return m;
    }
}
