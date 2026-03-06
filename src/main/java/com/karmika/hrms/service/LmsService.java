package com.karmika.hrms.service;

import com.karmika.hrms.entity.*;
import com.karmika.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LmsService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final VirtualClassRepository virtualClassRepository;
    private final EmployeeRepository employeeRepository;
    private final FileStorageService fileStorageService;

    // ─────────────────── HELPERS ────────────────────

    /** Parse a payload value to Long; returns null if missing or blank. */
    private Long safeToLong(Object val) {
        if (val == null)
            return null;
        String s = val.toString().trim();
        return s.isEmpty() ? null : Long.valueOf(s);
    }

    /** Parse a payload value to Integer; returns null if missing or blank. */
    private Integer safeToInt(Object val) {
        if (val == null)
            return null;
        String s = val.toString().trim();
        return s.isEmpty() ? null : Integer.valueOf(s);
    }

    // ─────────────────── COURSE ────────────────────

    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Course> getPublishedCourses() {
        return courseRepository.findByStatus(Course.CourseStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Course> searchCourses(String query) {
        return query == null || query.isBlank()
                ? courseRepository.findAll()
                : courseRepository.search(query.trim());
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return courseRepository.findAllCategories();
    }

    @Transactional
    public Course createCourse(Map<String, Object> payload, Long createdByEmployeeId) {
        Employee creator = employeeRepository.findById(createdByEmployeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Course course = new Course();
        course.setTitle((String) payload.get("title"));
        course.setDescription((String) payload.get("description"));
        course.setCategory((String) payload.getOrDefault("category", "General"));
        course.setTags((String) payload.get("tags"));
        course.setCreatedBy(creator);

        Long instructorId = safeToLong(payload.get("instructorId"));
        if (instructorId != null) {
            employeeRepository.findById(instructorId).ifPresent(course::setInstructor);
        }

        if (payload.get("level") != null) {
            course.setLevel(Course.Level.valueOf(payload.get("level").toString()));
        }
        Integer dh = safeToInt(payload.get("durationHours"));
        if (dh != null)
            course.setDurationHours(dh);
        if (payload.get("certificateEnabled") != null) {
            course.setCertificateEnabled(Boolean.valueOf(payload.get("certificateEnabled").toString()));
        }
        if (payload.get("thumbnailUrl") != null) {
            course.setThumbnailUrl((String) payload.get("thumbnailUrl"));
        }

        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Long id, Map<String, Object> payload) {
        Course course = getCourse(id);

        if (payload.containsKey("title"))
            course.setTitle((String) payload.get("title"));
        if (payload.containsKey("description"))
            course.setDescription((String) payload.get("description"));
        if (payload.containsKey("category"))
            course.setCategory((String) payload.get("category"));
        if (payload.containsKey("tags"))
            course.setTags((String) payload.get("tags"));
        if (payload.containsKey("durationHours")) {
            Integer dhu = safeToInt(payload.get("durationHours"));
            if (dhu != null)
                course.setDurationHours(dhu);
        }
        if (payload.containsKey("certificateEnabled"))
            course.setCertificateEnabled(Boolean.valueOf(payload.get("certificateEnabled").toString()));
        if (payload.containsKey("thumbnailUrl"))
            course.setThumbnailUrl((String) payload.get("thumbnailUrl"));
        if (payload.containsKey("level"))
            course.setLevel(Course.Level.valueOf(payload.get("level").toString()));
        if (payload.containsKey("status"))
            course.setStatus(Course.CourseStatus.valueOf(payload.get("status").toString()));

        if (payload.containsKey("instructorId")) {
            Long iid = safeToLong(payload.get("instructorId"));
            if (iid != null)
                employeeRepository.findById(iid).ifPresent(course::setInstructor);
        }

        return courseRepository.save(course);
    }

    @Transactional
    public void publishCourse(Long id) {
        Course course = getCourse(id);
        course.setStatus(Course.CourseStatus.PUBLISHED);
        courseRepository.save(course);
    }

    @Transactional
    public void archiveCourse(Long id) {
        Course course = getCourse(id);
        course.setStatus(Course.CourseStatus.ARCHIVED);
        courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    // ─────────────────── LESSON ────────────────────

    @Transactional(readOnly = true)
    public List<Lesson> getLessons(Long courseId) {
        return lessonRepository.findByCourseIdOrderByLessonOrderAsc(courseId);
    }

    @Transactional
    public Lesson addLesson(Long courseId, Map<String, Object> payload, MultipartFile file) throws IOException {
        Course course = getCourse(courseId);
        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setTitle((String) payload.get("title"));
        lesson.setDescription((String) payload.get("description"));
        if (payload.get("type") != null) {
            lesson.setType(Lesson.LessonType.valueOf(payload.get("type").toString()));
        }
        Integer dm = safeToInt(payload.get("durationMinutes"));
        if (dm != null)
            lesson.setDurationMinutes(dm);
        if (payload.get("isFree") != null) {
            lesson.setIsFree(Boolean.valueOf(payload.get("isFree").toString()));
        }

        // File upload takes priority; otherwise use provided URL
        if (file != null && !file.isEmpty()) {
            String savedUrl = fileStorageService.saveFile(file, "lms/lessons");
            lesson.setContentUrl(savedUrl);
            lesson.setOriginalFileName(file.getOriginalFilename());
        } else if (payload.get("contentUrl") != null) {
            lesson.setContentUrl((String) payload.get("contentUrl"));
        }

        // Set order to last
        long count = lessonRepository.countByCourseId(courseId);
        lesson.setLessonOrder((int) count + 1);

        return lessonRepository.save(lesson);
    }

    @Transactional
    public Lesson updateLesson(Long lessonId, Map<String, Object> payload, MultipartFile file) throws IOException {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        if (payload.containsKey("title"))
            lesson.setTitle((String) payload.get("title"));
        if (payload.containsKey("description"))
            lesson.setDescription((String) payload.get("description"));
        if (payload.containsKey("type"))
            lesson.setType(Lesson.LessonType.valueOf(payload.get("type").toString()));
        if (payload.containsKey("durationMinutes")) {
            Integer dmu = safeToInt(payload.get("durationMinutes"));
            if (dmu != null)
                lesson.setDurationMinutes(dmu);
        }
        if (payload.containsKey("isFree"))
            lesson.setIsFree(Boolean.valueOf(payload.get("isFree").toString()));
        if (payload.containsKey("contentUrl"))
            lesson.setContentUrl((String) payload.get("contentUrl"));

        if (file != null && !file.isEmpty()) {
            String savedUrl = fileStorageService.saveFile(file, "lms/lessons");
            lesson.setContentUrl(savedUrl);
            lesson.setOriginalFileName(file.getOriginalFilename());
        }

        return lessonRepository.save(lesson);
    }

    @Transactional
    public void deleteLesson(Long lessonId) {
        lessonRepository.deleteById(lessonId);
    }

    // ─────────────────── ENROLLMENT ────────────────────

    @Transactional
    public Enrollment enroll(Long courseId, Long employeeId) {
        if (enrollmentRepository.existsByCourseIdAndEmployeeId(courseId, employeeId)) {
            return enrollmentRepository.findByCourseIdAndEmployeeId(courseId, employeeId).get();
        }
        Course course = getCourse(courseId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setEmployee(employee);
        enrollment.setStatus(Enrollment.EnrollmentStatus.ENROLLED);
        return enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getMyEnrollments(Long employeeId) {
        return enrollmentRepository.findByEmployeeId(employeeId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getCourseEnrollments(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public Enrollment getEnrollmentById(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
    }

    @Transactional
    public Enrollment updateProgress(Long courseId, Long employeeId, int percent) {
        Enrollment enrollment = enrollmentRepository.findByCourseIdAndEmployeeId(courseId, employeeId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        // Block progress changes once course is completed
        if (enrollment.getStatus() == Enrollment.EnrollmentStatus.COMPLETED) {
            throw new RuntimeException("Course already completed. Request revisit to continue learning.");
        }

        enrollment.setProgressPercent(Math.min(100, Math.max(0, percent)));
        if (percent >= 100) {
            enrollment.setStatus(Enrollment.EnrollmentStatus.COMPLETED);
            enrollment.setCompletedAt(LocalDateTime.now());
            // Auto-generate certificate URL
            enrollment.setCertificateUrl("/api/lms/certificate/" + enrollment.getId());
        } else if (percent > 0) {
            enrollment.setStatus(Enrollment.EnrollmentStatus.IN_PROGRESS);
        }
        return enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void dropCourse(Long courseId, Long employeeId) {
        enrollmentRepository.findByCourseIdAndEmployeeId(courseId, employeeId)
                .ifPresent(e -> {
                    e.setStatus(Enrollment.EnrollmentStatus.DROPPED);
                    enrollmentRepository.save(e);
                });
    }

    /** Learner requests revisit access after completing a course */
    @Transactional
    public Enrollment requestRevisit(Long courseId, Long employeeId) {
        Enrollment enrollment = enrollmentRepository.findByCourseIdAndEmployeeId(courseId, employeeId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        if (enrollment.getStatus() != Enrollment.EnrollmentStatus.COMPLETED) {
            throw new RuntimeException("Only completed courses can be revisited");
        }
        enrollment.setStatus(Enrollment.EnrollmentStatus.REVISIT_REQUESTED);
        return enrollmentRepository.save(enrollment);
    }

    /**
     * Admin/HR approves revisit — resets progress so learner can go through again
     */
    @Transactional
    public Enrollment approveRevisit(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        enrollment.setStatus(Enrollment.EnrollmentStatus.IN_PROGRESS);
        enrollment.setProgressPercent(0);
        enrollment.setCompletedAt(null);
        enrollment.setCertificateUrl(null);
        return enrollmentRepository.save(enrollment);
    }

    // ─────────────────── VIRTUAL CLASS (GOOGLE MEET) ────────────────────

    @Transactional(readOnly = true)
    public List<VirtualClass> getVirtualClasses(Long courseId) {
        return virtualClassRepository.findByCourseIdOrderByScheduledAtAsc(courseId);
    }

    @Transactional(readOnly = true)
    public List<VirtualClass> getUpcomingSessions() {
        return virtualClassRepository.findByScheduledAtAfterOrderByScheduledAtAsc(LocalDateTime.now());
    }

    @Transactional
    public VirtualClass scheduleVirtualClass(Long courseId, Map<String, Object> payload, Long hostEmployeeId) {
        Course course = getCourse(courseId);
        Employee host = employeeRepository.findById(hostEmployeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        VirtualClass vc = new VirtualClass();
        vc.setCourse(course);
        vc.setHost(host);
        vc.setTitle((String) payload.get("title"));
        vc.setAgenda((String) payload.get("agenda"));
        vc.setMeetingLink((String) payload.get("meetingLink"));
        vc.setPlatform((String) payload.getOrDefault("platform", "google_meet"));
        if (payload.get("externalEventId") != null)
            vc.setExternalEventId((String) payload.get("externalEventId"));
        if (payload.get("scheduledAt") != null) {
            vc.setScheduledAt(LocalDateTime.parse(payload.get("scheduledAt").toString()));
        }
        Integer vcDm = safeToInt(payload.get("durationMinutes"));
        if (vcDm != null)
            vc.setDurationMinutes(vcDm);

        return virtualClassRepository.save(vc);
    }

    @Transactional
    public VirtualClass updateVirtualClass(Long vcId, Map<String, Object> payload) {
        VirtualClass vc = virtualClassRepository.findById(vcId)
                .orElseThrow(() -> new RuntimeException("Virtual class not found"));

        if (payload.containsKey("title"))
            vc.setTitle((String) payload.get("title"));
        if (payload.containsKey("agenda"))
            vc.setAgenda((String) payload.get("agenda"));
        if (payload.containsKey("meetingLink"))
            vc.setMeetingLink((String) payload.get("meetingLink"));
        if (payload.containsKey("platform"))
            vc.setPlatform((String) payload.get("platform"));
        if (payload.containsKey("scheduledAt"))
            vc.setScheduledAt(LocalDateTime.parse(payload.get("scheduledAt").toString()));
        if (payload.containsKey("durationMinutes")) {
            Integer vcdmu = safeToInt(payload.get("durationMinutes"));
            if (vcdmu != null)
                vc.setDurationMinutes(vcdmu);
        }
        if (payload.containsKey("status"))
            vc.setStatus(VirtualClass.SessionStatus.valueOf(payload.get("status").toString()));
        if (payload.containsKey("recordingUrl"))
            vc.setRecordingUrl((String) payload.get("recordingUrl"));

        return virtualClassRepository.save(vc);
    }

    @Transactional
    public void deleteVirtualClass(Long vcId) {
        virtualClassRepository.deleteById(vcId);
    }

    // ─────────────────── STATS ────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getLmsStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCourses", courseRepository.count());
        stats.put("publishedCourses", courseRepository.findByStatus(Course.CourseStatus.PUBLISHED).size());
        stats.put("totalEnrollments", enrollmentRepository.count());
        stats.put("totalUpcomingSessions", virtualClassRepository
                .findByScheduledAtAfterOrderByScheduledAtAsc(LocalDateTime.now()).size());
        stats.put("categories", courseRepository.findAllCategories());
        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyLmsStats(Long employeeId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<Enrollment> enrollments = enrollmentRepository.findByEmployeeId(employeeId);
        stats.put("totalEnrolled", enrollments.size());
        stats.put("completed",
                enrollments.stream().filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.COMPLETED).count());
        stats.put("inProgress",
                enrollments.stream().filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.IN_PROGRESS).count());
        return stats;
    }
}
