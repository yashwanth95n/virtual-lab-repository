package com.ledger.controller;

import com.ledger.config.FileStorage;
import com.ledger.config.GradeUtil;
import com.ledger.config.DockerLabService;
import com.ledger.model.PlatformSettings;
import com.ledger.repository.PlatformSettingsRepository;
import com.ledger.repository.LabActivityRepository;
import com.ledger.model.LabActivity;
import com.ledger.model.*;
import com.ledger.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final AssignmentRepository assignmentRepo;
    private final QuizRepository quizRepo;
    private final QuizQuestionRepository questionRepo;
    private final NotificationRepository notificationRepo;
    private final CourseMaterialRepository materialRepo;
    private final AssignmentSubmissionRepository submissionRepo;
    private final LabVmRepository labVmRepo;
    private final QuizAttemptRepository quizAttemptRepo;
    private final MaterialProgressRepository materialProgressRepo;
    private final ChatMessageRepository chatRepo;
    private final ExamAttemptRepository examAttemptRepo;
    private final DockerLabService dockerLabService;
    private final ExamRepository examRepo;
    private final LabRepository labRepo;
    private final LabSubmissionRepository labSubmissionRepo;
    private final FileStorage fileStorage;
    private final PlatformSettingsRepository settingsRepo;
    private final LabActivityRepository labActivityRepo;

    public StudentController(CourseRepository courseRepo, EnrollmentRepository enrollmentRepo,
                             AssignmentRepository assignmentRepo, QuizRepository quizRepo,
                             QuizQuestionRepository questionRepo, NotificationRepository notificationRepo,
                             CourseMaterialRepository materialRepo, AssignmentSubmissionRepository submissionRepo,
                             LabVmRepository labVmRepo, QuizAttemptRepository quizAttemptRepo,
                             MaterialProgressRepository materialProgressRepo,
                             ChatMessageRepository chatRepo,
                             ExamAttemptRepository examAttemptRepo,
                             ExamRepository examRepo,
                             LabRepository labRepo, LabSubmissionRepository labSubmissionRepo,
                             FileStorage fileStorage,
                             DockerLabService dockerLabService,
                             PlatformSettingsRepository settingsRepo,
                             LabActivityRepository labActivityRepo) {
        this.courseRepo = courseRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.assignmentRepo = assignmentRepo;
        this.quizRepo = quizRepo;
        this.questionRepo = questionRepo;
        this.notificationRepo = notificationRepo;
        this.materialRepo = materialRepo;
        this.submissionRepo = submissionRepo;
        this.labVmRepo = labVmRepo;
        this.quizAttemptRepo = quizAttemptRepo;
        this.materialProgressRepo = materialProgressRepo;
        this.chatRepo = chatRepo;
        this.examAttemptRepo = examAttemptRepo;
        this.dockerLabService = dockerLabService;
        this.examRepo = examRepo;
        this.labRepo = labRepo;
        this.labSubmissionRepo = labSubmissionRepo;
        this.fileStorage = fileStorage;
        this.settingsRepo = settingsRepo;
        this.labActivityRepo = labActivityRepo;
    }

    private boolean portalEnabled() {
        return settingsRepo.findAll().stream().findFirst()
                .map(PlatformSettings::isStudentPortalEnabled).orElse(true);
    }

    private User requireStudent(HttpSession session) {
        if (!portalEnabled()) return null;
        User u = (User) session.getAttribute("studentUser");
        if (u == null || !"STUDENT".equals(u.getRole())) return null;
        return u;
    }

    private String branchOf(User s) {
        if (s.getBranch() != null && !s.getBranch().isBlank()) return s.getBranch();
        if (s.getDepartment() != null) return s.getDepartment();
        return s.getCourseName();
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        List<Enrollment> myEnrollments = enrollmentRepo.findByStudentId(student.getId());
        // Top 3 courses by progress (highest first)
        List<Enrollment> topCourses = myEnrollments.stream()
                .sorted(Comparator.comparingInt(Enrollment::getProgress).reversed())
                .limit(3)
                .collect(Collectors.toList());
        String br = branchOf(student);
        List<Course> available = courseRepo.findAll().stream()
                .filter(c -> c.isPublished() || "Published".equalsIgnoreCase(c.getStatus()))
                .filter(c -> !"Inactive".equalsIgnoreCase(c.getStatus()))
                .filter(c -> br == null || br.isBlank() || br.equalsIgnoreCase(c.getCategory()))
                .collect(Collectors.toList());
        List<QuizAttempt> attempts = quizAttemptRepo.findByStudentId(student.getId());
        // Only the single most recent quiz attempt on dashboard
        List<QuizAttempt> latestQuizAttempts = attempts.stream()
                .sorted(Comparator.comparing(QuizAttempt::getAttemptedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(1)
                .collect(Collectors.toList());
        // Only the single most recent exam attempt on dashboard
        List<ExamAttempt> examResults = examAttemptRepo.findByStudentId(student.getId()).stream()
                .filter(a -> a.getTotal() > 0)
                .sorted(Comparator.comparing(ExamAttempt::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(1)
                .collect(Collectors.toList());
        // Overall grade from latest graded exam only (20-mark scale)
        String overallGrade = "—";
        if (!examResults.isEmpty() && examResults.get(0).getLetterGrade() != null
                && !"—".equals(examResults.get(0).getLetterGrade())) {
            overallGrade = examResults.get(0).getLetterGrade();
        }
        long enrolledCount = myEnrollments.size();
        model.addAttribute("student", student);
        model.addAttribute("enrollments", topCourses);
        model.addAttribute("enrolledCount", enrolledCount);
        model.addAttribute("totalCoursesCount", available.size());
        model.addAttribute("availableCourses", available);
        model.addAttribute("overallGrade", overallGrade);
        model.addAttribute("avgPercent", 0);
        long unread = chatRepo.findAll().stream()
                .filter(m -> student.getId().equals(m.getToUserId()) && "ADMIN".equals(m.getFromRole()) && !m.isReadFlag())
                .count();
        model.addAttribute("unreadChat", unread);
        model.addAttribute("examResults", examResults);
        model.addAttribute("latestQuizAttempts", latestQuizAttempts);
        String aud = "STUDENT:" + student.getId();
        List<Notification> notes = notificationRepo.findAll().stream()
                .filter(n -> n.getAudience() == null
                        || "All".equalsIgnoreCase(n.getAudience())
                        || "STUDENT".equalsIgnoreCase(n.getAudience())
                        || aud.equals(n.getAudience()))
                .collect(Collectors.toList());
        model.addAttribute("notifications", notes);
        model.addAttribute("activePage", "dashboard");
        return "student/dashboard";
    }

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false, defaultValue = "all") String filter, HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        String br = branchOf(student);
        List<Course> published = courseRepo.findAll().stream()
                .filter(c -> c.isPublished() || "Published".equalsIgnoreCase(c.getStatus()))
                .filter(c -> !"Inactive".equalsIgnoreCase(c.getStatus()) && !"Draft".equalsIgnoreCase(c.getStatus()) && !"Archived".equalsIgnoreCase(c.getStatus()))
                .filter(c -> br == null || br.isBlank() || br.equalsIgnoreCase(c.getCategory()))
                .collect(Collectors.toList());
        Set<Long> enrolledIds = enrollmentRepo.findByStudentId(student.getId()).stream()
                .map(Enrollment::getCourseId).collect(Collectors.toSet());
        // filter: all | enrolled
        if ("enrolled".equalsIgnoreCase(filter)) {
            published = published.stream().filter(c -> enrolledIds.contains(c.getId())).collect(Collectors.toList());
        }
        model.addAttribute("student", student);
        model.addAttribute("courses", published);
        model.addAttribute("enrolledIds", enrolledIds);
        model.addAttribute("filter", filter == null ? "all" : filter);
        model.addAttribute("activePage", "courses");
        return "student/courses";
    }

    @PostMapping("/courses/{id}/enroll")
    public String enroll(@PathVariable Long id, HttpSession session) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        Course course = courseRepo.findById(id).orElse(null);
        String br = branchOf(student);
        if (course != null && course.isPublished()
                && br != null && br.equalsIgnoreCase(course.getCategory())) {
            boolean already = enrollmentRepo.findByStudentId(student.getId()).stream()
                    .anyMatch(e -> e.getCourseId().equals(id));
            if (!already) {
                enrollmentRepo.save(new Enrollment(student.getId(), student.getName(), id, course.getTitle()));
                course.setEnrolledCount(course.getEnrolledCount() + 1);
                courseRepo.save(course);
            }
        }
        return "redirect:/student/enrollments";
    }

    @GetMapping("/enrollments")
    public String enrollments(HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        List<Enrollment> list = enrollmentRepo.findByStudentId(student.getId());
        Map<Long, Course> courseMap = new HashMap<>();
        for (Enrollment e : list) {
            courseRepo.findById(e.getCourseId()).ifPresent(c -> courseMap.put(e.getCourseId(), c));
        }
        model.addAttribute("student", student);
        model.addAttribute("enrollments", list);
        model.addAttribute("courseMap", courseMap);
        model.addAttribute("activePage", "enrollments");
        return "student/enrollments";
    }

    @GetMapping("/course/{id}")
    public String viewCourse(@PathVariable Long id, HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        Course course = courseRepo.findById(id).orElse(null);
        if (course == null || "Inactive".equalsIgnoreCase(course.getStatus()) || "Draft".equalsIgnoreCase(course.getStatus())) {
            return "redirect:/student/courses";
        }
        List<CourseMaterial> materials = materialRepo.findAll().stream()
                .filter(m -> m.getCourse() != null && m.getCourse().getId().equals(id))
                .sorted(Comparator.comparingInt(CourseMaterial::getOrderIndex))
                .collect(Collectors.toList());
        Set<Long> done = materialProgressRepo.findByStudentIdAndCourseId(student.getId(), id).stream()
                .filter(MaterialProgress::isCompleted)
                .map(MaterialProgress::getMaterialId)
                .collect(Collectors.toSet());
        model.addAttribute("student", student);
        model.addAttribute("course", course);
        model.addAttribute("materials", materials);
        model.addAttribute("completedIds", done);
        model.addAttribute("activePage", "courses");
        model.addAttribute("courseMode", true);
        model.addAttribute("focusId", null);
        return "student/course-view";
    }

    @GetMapping("/course/{id}/material/{mid}")
    public String viewMaterial(@PathVariable Long id, @PathVariable Long mid, HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        Course course = courseRepo.findById(id).orElse(null);
        if (course == null) return "redirect:/student/courses";
        List<CourseMaterial> materials = materialRepo.findAll().stream()
                .filter(m -> m.getCourse() != null && m.getCourse().getId().equals(id))
                .sorted(Comparator.comparingInt(CourseMaterial::getOrderIndex))
                .collect(Collectors.toList());
        Set<Long> done = materialProgressRepo.findByStudentIdAndCourseId(student.getId(), id).stream()
                .filter(MaterialProgress::isCompleted)
                .map(MaterialProgress::getMaterialId)
                .collect(Collectors.toSet());
        model.addAttribute("student", student);
        model.addAttribute("course", course);
        model.addAttribute("materials", materials);
        model.addAttribute("completedIds", done);
        model.addAttribute("focusId", mid);
        model.addAttribute("activePage", "courses");
        model.addAttribute("courseMode", true);
        return "student/course-view";
    }

    @PostMapping("/course/{courseId}/material/{materialId}/complete")
    public String completeMaterial(@PathVariable Long courseId, @PathVariable Long materialId,
                                   HttpSession session) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        MaterialProgress mp = materialProgressRepo.findByStudentIdAndMaterialId(student.getId(), materialId)
                .orElse(new MaterialProgress());
        mp.setStudentId(student.getId());
        mp.setCourseId(courseId);
        mp.setMaterialId(materialId);
        mp.setCompleted(true);
        mp.setCompletedAt(LocalDateTime.now());
        materialProgressRepo.save(mp);
        // update enrollment progress
        List<CourseMaterial> all = materialRepo.findAll().stream()
                .filter(m -> m.getCourse() != null && m.getCourse().getId().equals(courseId))
                .collect(Collectors.toList());
        long done = materialProgressRepo.countByStudentIdAndCourseIdAndCompletedTrue(student.getId(), courseId);
        int pct = all.isEmpty() ? 0 : (int) Math.round(100.0 * done / all.size());
        enrollmentRepo.findByStudentId(student.getId()).stream()
                .filter(e -> courseId.equals(e.getCourseId()))
                .forEach(e -> {
                    e.setProgress(pct);
                    if (pct >= 100) e.setStatus("Completed");
                    enrollmentRepo.save(e);
                });
        return "redirect:/student/course/" + courseId;
    }

    @GetMapping("/assessments")
    public String assessments(@RequestParam(required = false, defaultValue = "assignments") String tab, HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        String br = branchOf(student);
        List<Assignment> assignments = assignmentRepo.findAll().stream()
                .filter(a -> "Published".equals(a.getStatus()))
                .filter(a -> a.getBranch() == null || a.getBranch().isBlank()
                        || (br != null && br.equalsIgnoreCase(a.getBranch())))
                .collect(Collectors.toList());
        List<Quiz> quizzes = quizRepo.findAll().stream()
                .filter(q -> "Published".equals(q.getStatus()))
                .filter(q -> q.getCategory() == null || "All".equalsIgnoreCase(q.getCategory())
                        || (br != null && br.equalsIgnoreCase(q.getCategory())))
                .collect(Collectors.toList());
        List<Exam> exams = examRepo.findAll().stream()
                .filter(e -> "Published".equalsIgnoreCase(e.getStatus()))
                .filter(e -> e.getBranch() == null || "All".equalsIgnoreCase(e.getBranch())
                        || (br != null && br.equalsIgnoreCase(e.getBranch())))
                .collect(Collectors.toList());
        List<ExamAttempt> myExamAttempts = examAttemptRepo.findByStudentId(student.getId());
        Map<Long, ExamAttempt> examAttemptByExam = new HashMap<>();
        for (ExamAttempt ea : myExamAttempts) {
            ExamAttempt prev = examAttemptByExam.get(ea.getExamId());
            if (prev == null || (ea.getSubmittedAt() != null && (prev.getSubmittedAt() == null || ea.getSubmittedAt().isAfter(prev.getSubmittedAt())))) {
                examAttemptByExam.put(ea.getExamId(), ea);
            }
        }
        model.addAttribute("student", student);
        model.addAttribute("assignments", assignments);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("exams", exams);
        model.addAttribute("examAttemptByExam", examAttemptByExam);
        model.addAttribute("submissions", submissionRepo.findByStudentId(student.getId()));
        model.addAttribute("tab", tab);
        model.addAttribute("activePage", "assessments");
        return "student/assessments";
    }

    @GetMapping("/assessments/assignment/{id}")
    public String viewAssignment(@PathVariable Long id, HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        Assignment a = assignmentRepo.findById(id).orElse(null);
        if (a == null) return "redirect:/student/assessments";
        if (a.getDueDate() != null && a.getDueDate().isBefore(java.time.LocalDate.now())) {
            return "redirect:/student/assessments?tab=assignments&closed=1";
        }
        model.addAttribute("student", student);
        model.addAttribute("assignment", a);
        model.addAttribute("activePage", "assessments");
        return "student/assignment-view";
    }

    @PostMapping("/assessments/assignment/{id}/submit")
    public String submitAssignment(@PathVariable Long id,
                                   @RequestParam(required = false) MultipartFile answerFile,
                                   HttpSession session) throws IOException {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        Assignment a = assignmentRepo.findById(id).orElse(null);
        if (a == null) return "redirect:/student/assessments";
        if (a.getDueDate() != null && a.getDueDate().isBefore(java.time.LocalDate.now())) {
            return "redirect:/student/assessments?tab=assignments&closed=1";
        }
        AssignmentSubmission sub = new AssignmentSubmission();
        sub.setAssignmentId(a.getId());
        sub.setAssignmentTitle(a.getTitle());
        sub.setStudentId(student.getId());
        sub.setStudentName(student.getName());
        sub.setInstructorId(a.getInstructorId());
        sub.setStatus("Submitted");
        sub.setSubmittedAt(LocalDateTime.now());
        if (answerFile != null && !answerFile.isEmpty()) {
            sub.setFilePath(fileStorage.store(answerFile, "submissions"));
        }
        submissionRepo.save(sub);
        a.setSubmissions(a.getSubmissions() + 1);
        assignmentRepo.save(a);
        return "redirect:/student/assessments";
    }

    
    @PostMapping("/assessments/submission/{id}/delete")
    public String deleteSubmission(@PathVariable Long id, HttpSession session) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        submissionRepo.findById(id).ifPresent(sub -> {
            if (student.getId().equals(sub.getStudentId())) {
                submissionRepo.delete(sub);
            }
        });
        return "redirect:/student/assessments?tab=submitted";
    }

    @GetMapping("/quiz/{id}")
    public String takeQuiz(@PathVariable Long id, HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";

        Quiz quiz = quizRepo.findById(id).orElse(null);
        if (quiz == null || !"Published".equals(quiz.getStatus())) {
            return "redirect:/student/assessments";
        }

        List<QuizAttempt> quizAttempts = quizAttemptRepo.findByStudentId(student.getId()).stream()
                .filter(a -> id.equals(a.getQuizId()))
                .sorted(Comparator.comparing(
                        QuizAttempt::getAttemptedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .collect(Collectors.toList());

        long used = quizAttempts.size();

        int max = quiz.getMaxAttempts() > 0 ? quiz.getMaxAttempts() : 2;

        /*
         * Quiz rule:
         * - Maximum 2 attempts.
         * - If the first attempt is >= 70%, no second attempt.
         * - If the first attempt is < 70%, second attempt is allowed.
         */
        boolean firstAttemptPassed = !quizAttempts.isEmpty()
                && quizAttempts.get(0).getPercent() >= 70;

        if (used >= max || used >= 2 || firstAttemptPassed) {
            model.addAttribute("student", student);
            model.addAttribute("quiz", quiz);
            model.addAttribute("used", used);
            model.addAttribute("max", Math.min(max, 2));
            model.addAttribute("blocked", true);
            model.addAttribute("activePage", "assessments");
            return "student/quiz-blocked";
        }

        List<QuizQuestion> questions = questionRepo.findAll().stream()
                .filter(q -> q.getQuiz() != null && q.getQuiz().getId().equals(id))
                .collect(Collectors.toList());

        // Always randomize order for each student attempt
        Collections.shuffle(questions);

        model.addAttribute("student", student);
        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", questions);
        model.addAttribute("used", used);
        model.addAttribute("max", Math.min(max, 2));
        model.addAttribute("activePage", "assessments");

        return "student/quiz-take";
    }

    @PostMapping("/quiz/{id}/submit")
    public String submitQuiz(@PathVariable Long id, HttpSession session,
                             @RequestParam Map<String, String> allParams, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        Quiz quiz = quizRepo.findById(id).orElse(null);
        if (quiz == null) return "redirect:/student/assessments";

        // Enforce maximum 2 attempts and the 70% first-attempt rule
        List<QuizAttempt> previousAttempts = quizAttemptRepo.findByStudentId(student.getId()).stream()
                .filter(a -> id.equals(a.getQuizId()))
                .sorted(Comparator.comparing(
                        QuizAttempt::getAttemptedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .collect(Collectors.toList());

        long used = previousAttempts.size();

        int max = quiz.getMaxAttempts() > 0 ? Math.min(quiz.getMaxAttempts(), 2) : 2;

        boolean firstAttemptPassed = !previousAttempts.isEmpty()
                && previousAttempts.get(0).getPercent() >= 70;

        if (used >= max || used >= 2 || firstAttemptPassed) {
            return "redirect:/student/quiz/" + id;
        }
        List<QuizQuestion> questions = questionRepo.findAll().stream()
                .filter(q -> q.getQuiz() != null && q.getQuiz().getId().equals(id))
                .collect(Collectors.toList());
        int correct = 0;
        List<Map<String, Object>> review = new ArrayList<>();
        for (QuizQuestion q : questions) {
            String ans = allParams.get("q_" + q.getId());
            boolean ok = ans != null && ans.equalsIgnoreCase(q.getCorrectOption());
            if (ok) correct++;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("question", q.getQuestionText());
            row.put("yourAnswer", ans != null ? ans : "—");
            row.put("correctAnswer", q.getCorrectOption());
            row.put("ok", ok);
            review.add(row);
        }
        int total = questions.size();
        double percent = GradeUtil.isGradable(total) ? GradeUtil.percentFromMarks(correct, total) : -1;
        String letter = "—"; // no letter grade for quizzes

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuizId(quiz.getId());
        attempt.setQuizTitle(quiz.getTitle());
        attempt.setStudentId(student.getId());
        attempt.setStudentName(student.getName());
        attempt.setBranch(branchOf(student));
        attempt.setScore(correct);
        attempt.setTotal(total);
        attempt.setPercent(percent);
        attempt.setLetterGrade(letter);
        attempt.setAttemptedAt(LocalDateTime.now());
        quizAttemptRepo.save(attempt);

        model.addAttribute("student", student);
        model.addAttribute("quiz", quiz);
        model.addAttribute("score", correct);
        model.addAttribute("total", total);
        model.addAttribute("percent", percent >= 0 ? Math.round(percent) : -1);
        model.addAttribute("letterGrade", letter);
        model.addAttribute("review", review);
        model.addAttribute("activePage", "assessments");
        return "student/quiz-result";
    }

    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        List<QuizAttempt> allAttempts = quizAttemptRepo.findByStudentId(student.getId());
        Map<Long, QuizAttempt> latestMap = new LinkedHashMap<>();
        for (QuizAttempt qa : allAttempts) {
            QuizAttempt prev = latestMap.get(qa.getQuizId());
            if (prev == null || (qa.getAttemptedAt() != null && (prev.getAttemptedAt() == null || qa.getAttemptedAt().isAfter(prev.getAttemptedAt())))) {
                latestMap.put(qa.getQuizId(), qa);
            }
        }
        List<QuizAttempt> attempts = new ArrayList<>(latestMap.values());
        List<Enrollment> enrollments = enrollmentRepo.findByStudentId(student.getId());
        List<AssignmentSubmission> submissions = submissionRepo.findByStudentId(student.getId());
        Set<Long> publishedExamIds = examRepo.findAll().stream()
                .filter(Exam::isResultsPublished)
                .map(Exam::getId)
                .collect(Collectors.toSet());
        List<ExamAttempt> examAttempts = examAttemptRepo.findByStudentId(student.getId()).stream()
                .filter(a -> publishedExamIds.contains(a.getExamId()))
                .filter(a -> "Submitted".equals(a.getStatus()) || "Blocked".equals(a.getStatus()))
                .collect(Collectors.toList());
        // only published exam results
        double coursePct = enrollments.stream().mapToInt(Enrollment::getProgress).average().orElse(0);
        double quizPct = attempts.stream().filter(a -> a.getPercent() >= 0).mapToDouble(QuizAttempt::getPercent).average().orElse(0);
        double assignPct = submissions.isEmpty() ? 0 : 100.0; // submitted = complete for simple metric
        double examPct = examAttempts.stream().filter(a -> a.getPercent() >= 0).mapToDouble(ExamAttempt::getPercent).average().orElse(0);
        double overall = (coursePct + quizPct + assignPct + examPct) / 4.0;
        model.addAttribute("student", student);
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("submissions", submissions);
        model.addAttribute("attempts", attempts);
        model.addAttribute("examAttempts", examAttempts);
        model.addAttribute("coursePct", (int) Math.round(coursePct));
        model.addAttribute("quizPct", (int) Math.round(quizPct));
        model.addAttribute("assignPct", (int) Math.round(assignPct));
        model.addAttribute("examPct", (int) Math.round(examPct));
        model.addAttribute("overallPct", (int) Math.round(overall));
        // Grade only from published 20-mark exams
        String reportGrade = "—";
        if (!examAttempts.isEmpty()) {
            ExamAttempt latestGraded = examAttempts.stream()
                    .filter(a -> a.getLetterGrade() != null && !"—".equals(a.getLetterGrade()))
                    .findFirst().orElse(null);
            if (latestGraded != null) reportGrade = latestGraded.getLetterGrade();
        }
        model.addAttribute("overallGrade", reportGrade);
        model.addAttribute("activePage", "reports");
        return "student/reports";
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        model.addAttribute("student", student);
        model.addAttribute("notifications", notificationRepo.findAll());
        model.addAttribute("activePage", "notifications");
        return "student/notifications";
    }

    @GetMapping("/settings")
    public String settings(HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        model.addAttribute("student", student);
        model.addAttribute("activePage", "profile");
        return "student/settings";
    }

    @GetMapping("/lab")
    public String myLabs(HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        model.addAttribute("student", student);
        model.addAttribute("vms", labVmRepo.findByAssignedStudentId(student.getId()));
        model.addAttribute("activePage", "lab");
        return "student/lab";
    }

    @GetMapping("/lab/{id}")
    public String openLab(@PathVariable Long id, HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        LabVm vm = labVmRepo.findById(id).orElse(null);
        if (vm == null || !student.getId().equals(vm.getAssignedStudentId())) {
            return "redirect:/student/lab";
        }
        vm.setStatus("Running");
        labVmRepo.save(vm);
        model.addAttribute("student", student);
        model.addAttribute("vm", vm);
        model.addAttribute("activePage", "lab");
        return "student/lab-session";
    }

    @PostMapping("/lab/{id}/save")
    public String saveLabWork(@PathVariable Long id,
                              @RequestParam(required = false) MultipartFile workFile,
                              HttpSession session) throws IOException {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        LabVm vm = labVmRepo.findById(id).orElse(null);
        if (vm != null && student.getId().equals(vm.getAssignedStudentId())) {
            if (workFile != null && !workFile.isEmpty()) {
                vm.setSavedFilePath(fileStorage.store(workFile, "lab"));
            }
            vm.setLastSavedAt(LocalDateTime.now());
            labVmRepo.save(vm);
        }
        return "redirect:/student/lab/" + id + "?saved=1";
    }

    @GetMapping("/chat")
    public String chat(HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        model.addAttribute("student", student);
        model.addAttribute("messages", chatRepo.findByFromUserIdOrderBySentAtAsc(student.getId()));
        // also messages to this student from admin
        List<ChatMessage> all = chatRepo.findAll().stream()
                .filter(m -> student.getId().equals(m.getFromUserId()) || student.getId().equals(m.getToUserId()))
                .sorted(Comparator.comparing(ChatMessage::getSentAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        for (ChatMessage cm : all) {
            if ("ADMIN".equals(cm.getFromRole()) && !cm.isReadFlag()) {
                cm.setReadFlag(true);
                chatRepo.save(cm);
            }
        }
        model.addAttribute("messages", all);
        model.addAttribute("unreadChat", 0);
        model.addAttribute("activePage", "chat");
        return "student/chat";
    }

    @PostMapping("/chat/send")
    public String sendChat(@RequestParam String message, HttpSession session) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        ChatMessage m = new ChatMessage();
        m.setFromUserId(student.getId());
        m.setFromName(student.getName());
        m.setFromRole("STUDENT");
        m.setMessage(message);
        m.setSentAt(LocalDateTime.now());
        chatRepo.save(m);
        return "redirect:/student/chat";
    }

    @GetMapping("/labs")
    public String studentLabs(HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        String br = student.getBranch() != null ? student.getBranch() : student.getDepartment();
        List<Lab> labs = labRepo.findAll().stream()
                .filter(l -> "Published".equalsIgnoreCase(l.getStatus()))
                .filter(l -> br == null || br.isBlank() || "All".equalsIgnoreCase(l.getBranch())
                        || br.equalsIgnoreCase(l.getBranch()))
                .sorted(Comparator.comparing(Lab::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        List<LabVm> labVms = labVmRepo.findByAssignedStudentId(student.getId());
        model.addAttribute("student", student);
        model.addAttribute("labs", labs);
        model.addAttribute("labVms", labVms);
        model.addAttribute("activePage", "labs");
        return "student/labs";
    }

    @GetMapping("/labs/{id}")
    public String openCourseLab(@PathVariable Long id, HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        Lab lab = labRepo.findById(id).orElse(null);
        if (lab == null) return "redirect:/student/labs";
        model.addAttribute("student", student);
        model.addAttribute("lab", lab);
        model.addAttribute("activePage", "labs");
        return "student/lab-view";
    }

    @PostMapping("/labs/{id}/submit")
    public String submitLab(@PathVariable Long id,
                            @RequestParam(required = false) MultipartFile solutionFile,
                            HttpSession session) throws IOException {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        Lab lab = labRepo.findById(id).orElse(null);
        if (lab == null) return "redirect:/student/labs";
        LabSubmission sub = new LabSubmission();
        sub.setLabId(lab.getId());
        sub.setLabTitle(lab.getTitle());
        sub.setStudentId(student.getId());
        sub.setStudentName(student.getName());
        sub.setBranch(student.getBranch());
        sub.setStatus("Submitted");
        sub.setSubmittedAt(LocalDateTime.now());
        if (solutionFile != null && !solutionFile.isEmpty()) {
            sub.setFilePath(fileStorage.store(solutionFile, "lab-submissions"));
        }
        labSubmissionRepo.save(sub);
        return "redirect:/student/labs?submitted=1";
    }

    /** Launch Docker lab for assigned student — opens noVNC desktop. */
    @PostMapping("/lab/{id}/launch")
    public String studentLaunchLab(@PathVariable Long id, HttpSession session) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        LabVm vm = labVmRepo.findById(id).orElse(null);
        if (vm == null || !student.getId().equals(vm.getAssignedStudentId())) {
            return "redirect:/student/lab";
        }
        // Already running
        if (vm.getNovncUrl() != null && vm.getContainerId() != null) {
            return "redirect:" + vm.getNovncUrl();
        }
        boolean kali = vm.getOsType() != null && vm.getOsType().toLowerCase().contains("kali");
        DockerLabService.LaunchResult result = kali ? dockerLabService.launchKali() : dockerLabService.launchUbuntu();
        if (result.success) {
            vm.setContainerId(result.containerId);
            vm.setHostPort(result.port);
            vm.setNovncUrl(result.novncUrl);
            vm.setAccessUrl(result.novncUrl);
            vm.setStatus("Running");
            labVmRepo.save(vm);
            LabActivity act = new LabActivity();
            act.setLabVmId(vm.getId());
            act.setUserId(student.getId());
            act.setUserName(student.getName());
            act.setUserRole("STUDENT");
            act.setAction("LAUNCH");
            act.setDetails("Student launched " + (vm.getOsType() != null ? vm.getOsType() : "lab"));
            labActivityRepo.save(act);
            return "redirect:/student/lab/" + id + "/desktop";
        }
        return "redirect:/student/lab?error=docker";
    }

    /** Embedded noVNC Ubuntu/Kali desktop inside student portal. */
    @GetMapping("/lab/{id}/desktop")
    public String studentLabDesktop(@PathVariable Long id, HttpSession session, Model model) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        LabVm vm = labVmRepo.findById(id).orElse(null);
        if (vm == null || (vm.getAssignedStudentId() != null && !student.getId().equals(vm.getAssignedStudentId()))) {
            return "redirect:/student/lab";
        }
        if (vm.getNovncUrl() == null || vm.getNovncUrl().isBlank()) {
            return "redirect:/student/lab/" + id + "/launch-get";
        }
        model.addAttribute("student", student);
        model.addAttribute("vm", vm);
        model.addAttribute("novncUrl", vm.getNovncUrl());
        model.addAttribute("activePage", "labs");
        return "student/lab-desktop";
    }

    @GetMapping("/lab/{id}/launch-get")
    public String studentLaunchGet(@PathVariable Long id, HttpSession session) {
        // Allow GET open path to trigger same as POST when needed
        return "redirect:/student/lab";
    }

    @PostMapping("/lab/{id}/stop")
    public String studentStopLab(@PathVariable Long id, HttpSession session) {
        User student = requireStudent(session);
        if (student == null) return "redirect:/student/login";
        LabVm vm = labVmRepo.findById(id).orElse(null);
        if (vm == null || !student.getId().equals(vm.getAssignedStudentId())) {
            return "redirect:/student/lab";
        }
        if (vm.getContainerId() != null) {
            dockerLabService.stop(vm.getContainerId());
        }
        vm.setContainerId(null);
        vm.setHostPort(0);
        vm.setNovncUrl(null);
        vm.setStatus("Assigned");
        labVmRepo.save(vm);
        return "redirect:/student/lab?stopped=1";
    }
}
