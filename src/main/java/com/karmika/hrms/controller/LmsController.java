package com.karmika.hrms.controller;

import com.karmika.hrms.entity.*;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.service.LmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * LMS REST Controller
 * Base path: /api/lms
 */
@RestController
@RequestMapping("/api/lms")
@RequiredArgsConstructor
public class LmsController {

    private final LmsService lmsService;
    private final EmployeeRepository employeeRepository;

    // ─────────────────── HELPER ────────────────────

    private Employee resolveEmployee(Authentication auth) {
        String username = auth.getName();
        return employeeRepository.findAll().stream()
                .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Employee record not found for user: " + username));
    }

    // ─────────────────── STATS ────────────────────

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(Authentication auth) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", lmsService.getLmsStats()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/my-stats")
    public ResponseEntity<?> getMyStats(Authentication auth) {
        try {
            Employee employee = resolveEmployee(auth);
            return ResponseEntity.ok(Map.of("success", true, "data", lmsService.getMyLmsStats(employee.getId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ─────────────────── COURSES ────────────────────

    /**
     * GET /api/lms/courses — All courses (admin/HR see all; employees see
     * PUBLISHED)
     */
    @GetMapping("/courses")
    public ResponseEntity<?> getCourses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Authentication auth) {
        try {
            List<Course> courses;
            boolean isAdminOrHr = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR")
                            || a.getAuthority().equals("ROLE_MANAGER"));

            if (search != null && !search.isBlank()) {
                courses = lmsService.searchCourses(search);
            } else if (!isAdminOrHr) {
                courses = lmsService.getPublishedCourses();
            } else {
                courses = lmsService.getAllCourses();
            }

            return ResponseEntity.ok(Map.of("success", true, "courses", courses));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * GET /api/lms/courses/{id}
     */
    @GetMapping("/courses/{id}")
    public ResponseEntity<?> getCourse(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "course", lmsService.getCourse(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * GET /api/lms/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "categories", lmsService.getCategories()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * POST /api/lms/courses — Create course (ADMIN, HR, MANAGER)
     */
    @PostMapping("/courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> createCourse(@RequestBody Map<String, Object> payload, Authentication auth) {
        try {
            Employee employee = resolveEmployee(auth);
            Course course = lmsService.createCourse(payload, employee.getId());
            return ResponseEntity
                    .ok(Map.of("success", true, "message", "Course created successfully", "course", course));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * PUT /api/lms/courses/{id} — Update course (ADMIN, HR, MANAGER)
     */
    @PutMapping("/courses/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> updateCourse(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Course course = lmsService.updateCourse(id, payload);
            return ResponseEntity.ok(Map.of("success", true, "message", "Course updated", "course", course));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * POST /api/lms/courses/{id}/publish
     */
    @PostMapping("/courses/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> publishCourse(@PathVariable Long id) {
        try {
            lmsService.publishCourse(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Course published"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * POST /api/lms/courses/{id}/archive
     */
    @PostMapping("/courses/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> archiveCourse(@PathVariable Long id) {
        try {
            lmsService.archiveCourse(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Course archived"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/lms/courses/{id}
     */
    @DeleteMapping("/courses/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        try {
            lmsService.deleteCourse(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Course deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ─────────────────── LESSONS ────────────────────

    /**
     * GET /api/lms/courses/{courseId}/lessons
     */
    @GetMapping("/courses/{courseId}/lessons")
    public ResponseEntity<?> getLessons(@PathVariable Long courseId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "lessons", lmsService.getLessons(courseId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * POST /api/lms/courses/{courseId}/lessons (multipart — supports file upload)
     */
    @PostMapping(value = "/courses/{courseId}/lessons", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> addLesson(
            @PathVariable Long courseId,
            @RequestPart("data") Map<String, Object> payload,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Authentication auth) {
        try {
            Lesson lesson = lmsService.addLesson(courseId, payload, file);
            return ResponseEntity.ok(Map.of("success", true, "message", "Lesson added", "lesson", lesson));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * PUT /api/lms/lessons/{lessonId} (multipart)
     */
    @PutMapping(value = "/lessons/{lessonId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> updateLesson(
            @PathVariable Long lessonId,
            @RequestPart("data") Map<String, Object> payload,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            Lesson lesson = lmsService.updateLesson(lessonId, payload, file);
            return ResponseEntity.ok(Map.of("success", true, "message", "Lesson updated", "lesson", lesson));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/lms/lessons/{lessonId}
     */
    @DeleteMapping("/lessons/{lessonId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> deleteLesson(@PathVariable Long lessonId) {
        try {
            lmsService.deleteLesson(lessonId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Lesson deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ─────────────────── ENROLLMENTS ────────────────────

    /**
     * POST /api/lms/courses/{courseId}/enroll — Self-enroll
     */
    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<?> enroll(@PathVariable Long courseId, Authentication auth) {
        try {
            Employee employee = resolveEmployee(auth);
            Enrollment enrollment = lmsService.enroll(courseId, employee.getId());
            return ResponseEntity
                    .ok(Map.of("success", true, "message", "Enrolled successfully", "enrollment", enrollment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * GET /api/lms/my-enrollments
     */
    @GetMapping("/my-enrollments")
    public ResponseEntity<?> getMyEnrollments(Authentication auth) {
        try {
            Employee employee = resolveEmployee(auth);
            return ResponseEntity
                    .ok(Map.of("success", true, "enrollments", lmsService.getMyEnrollments(employee.getId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * GET /api/lms/courses/{courseId}/enrollments — (ADMIN, HR)
     */
    @GetMapping("/courses/{courseId}/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> getCourseEnrollments(@PathVariable Long courseId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "enrollments", lmsService.getCourseEnrollments(courseId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * PUT /api/lms/courses/{courseId}/progress — Update progress %
     * Body: { "percent": 50 }
     */
    @PutMapping("/courses/{courseId}/progress")
    public ResponseEntity<?> updateProgress(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Employee employee = resolveEmployee(auth);
            int percent = Integer.parseInt(body.get("percent").toString());
            Enrollment enrollment = lmsService.updateProgress(courseId, employee.getId(), percent);
            return ResponseEntity.ok(Map.of("success", true, "enrollment", enrollment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/lms/courses/{courseId}/enroll — Drop a course
     */
    @DeleteMapping("/courses/{courseId}/enroll")
    public ResponseEntity<?> dropCourse(@PathVariable Long courseId, Authentication auth) {
        try {
            Employee employee = resolveEmployee(auth);
            lmsService.dropCourse(courseId, employee.getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Dropped from course"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * GET /api/lms/certificate/{enrollmentId} — Download completion certificate
     */
    @GetMapping(value = "/certificate/{enrollmentId}", produces = "text/html")
    public ResponseEntity<?> getCertificate(@PathVariable Long enrollmentId) {
        try {
            var enrollment = lmsService.getEnrollmentById(enrollmentId);
            if (enrollment.getStatus() != com.karmika.hrms.entity.Enrollment.EnrollmentStatus.COMPLETED
                    && enrollment
                            .getStatus() != com.karmika.hrms.entity.Enrollment.EnrollmentStatus.REVISIT_REQUESTED) {
                return ResponseEntity.badRequest().body("Course not yet completed");
            }
            String empName = enrollment.getEmployee().getFirstName() + " " + enrollment.getEmployee().getLastName();
            String courseTitle = enrollment.getCourse().getTitle();
            String completedAt = enrollment.getCompletedAt() != null
                    ? enrollment.getCompletedAt().toLocalDate().toString()
                    : "N/A";

            String html = "<!DOCTYPE html><html><head><title>Certificate of Completion</title>"
                    + "<style>"
                    + "body{margin:0;display:flex;justify-content:center;align-items:center;min-height:100vh;background:#1a1a2e;font-family:'Segoe UI',sans-serif}"
                    + ".cert{width:900px;padding:60px;background:linear-gradient(135deg,#0f0c29,#302b63,#24243e);border:3px solid #f5c842;border-radius:20px;text-align:center;color:#fff;box-shadow:0 0 60px rgba(245,200,66,.2)}"
                    + ".cert h1{font-size:42px;color:#f5c842;margin-bottom:8px;letter-spacing:3px}"
                    + ".cert .sub{font-size:18px;color:#a0a0c0;margin-bottom:40px}"
                    + ".cert .name{font-size:36px;color:#fff;border-bottom:2px solid #f5c842;display:inline-block;padding-bottom:8px;margin:20px 0}"
                    + ".cert .course-name{font-size:24px;color:#f5c842;margin:20px 0}"
                    + ".cert .date{color:#a0a0c0;margin-top:30px;font-size:14px}"
                    + ".cert .seal{font-size:48px;margin-top:20px}"
                    + "@media print{body{background:#fff}.cert{box-shadow:none;border-color:#333;color:#333}.cert h1,.cert .course-name{color:#333}.cert .sub,.cert .date{color:#666}}"
                    + "</style></head><body>"
                    + "<div class='cert'>"
                    + "<h1>CERTIFICATE OF COMPLETION</h1>"
                    + "<div class='sub'>This is to certify that</div>"
                    + "<div class='name'>" + empName + "</div>"
                    + "<div class='sub'>has successfully completed the course</div>"
                    + "<div class='course-name'>\"" + courseTitle + "\"</div>"
                    + "<div class='date'>Completed on: " + completedAt + "</div>"
                    + "<div class='seal'>\uD83C\uDFC6</div>"
                    + "<div class='sub' style='margin-top:30px'>Karmika HRMS — Learning Management System</div>"
                    + "</div></body></html>";

            return ResponseEntity.ok(html);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * POST /api/lms/courses/{courseId}/revisit — Learner requests revisit access
     */
    @PostMapping("/courses/{courseId}/revisit")
    public ResponseEntity<?> requestRevisit(@PathVariable Long courseId, Authentication auth) {
        try {
            Employee employee = resolveEmployee(auth);
            Enrollment enrollment = lmsService.requestRevisit(courseId, employee.getId());
            return ResponseEntity
                    .ok(Map.of("success", true, "message", "Revisit request submitted", "enrollment", enrollment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * POST /api/lms/enrollments/{enrollmentId}/approve-revisit — Admin/HR approves
     */
    @PostMapping("/enrollments/{enrollmentId}/approve-revisit")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> approveRevisit(@PathVariable Long enrollmentId) {
        try {
            Enrollment enrollment = lmsService.approveRevisit(enrollmentId);
            return ResponseEntity
                    .ok(Map.of("success", true, "message", "Revisit approved — learner can access the course again"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ─────────────────── VIRTUAL CLASSES (GOOGLE MEET) ────────────────────

    /**
     * GET /api/lms/courses/{courseId}/sessions
     */
    @GetMapping("/courses/{courseId}/sessions")
    public ResponseEntity<?> getCourseSessions(@PathVariable Long courseId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "sessions", lmsService.getVirtualClasses(courseId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * GET /api/lms/sessions/upcoming — All upcoming sessions (all authenticated
     * users)
     */
    @GetMapping("/sessions/upcoming")
    public ResponseEntity<?> getUpcomingSessions() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "sessions", lmsService.getUpcomingSessions()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * POST /api/lms/courses/{courseId}/sessions — Schedule a live session (ADMIN,
     * HR, MANAGER)
     * Body: { title, agenda, meetingLink, platform, scheduledAt (ISO),
     * durationMinutes }
     */
    @PostMapping("/courses/{courseId}/sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> scheduleSession(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        try {
            Employee host = resolveEmployee(auth);
            VirtualClass vc = lmsService.scheduleVirtualClass(courseId, payload, host.getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Session scheduled", "session", vc));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * PUT /api/lms/sessions/{vcId} — Update session details
     */
    @PutMapping("/sessions/{vcId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> updateSession(@PathVariable Long vcId, @RequestBody Map<String, Object> payload) {
        try {
            VirtualClass vc = lmsService.updateVirtualClass(vcId, payload);
            return ResponseEntity.ok(Map.of("success", true, "message", "Session updated", "session", vc));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/lms/sessions/{vcId}
     */
    @DeleteMapping("/sessions/{vcId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> deleteSession(@PathVariable Long vcId) {
        try {
            lmsService.deleteVirtualClass(vcId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Session deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
