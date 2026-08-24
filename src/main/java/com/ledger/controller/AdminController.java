package com.ledger.controller;

import com.ledger.config.FileStorage;
import com.ledger.config.QuizTextParser;
import com.ledger.config.GradeUtil;
import com.ledger.config.DockerLabService;
import com.ledger.model.LabActivity;
import com.ledger.repository.LabActivityRepository;
import com.ledger.model.*;
import com.ledger.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final AssignmentRepository assignmentRepo;
    private final QuizRepository quizRepo;
    private final QuizQuestionRepository questionRepo;
    private final NotificationRepository notificationRepo;
    private final PlatformSettingsRepository settingsRepo;
    private final CourseMaterialRepository materialRepo;
    private final AssignmentSubmissionRepository submissionRepo;
    private final LabVmRepository labVmRepo;
    private final FileStorage fileStorage;
    private final BranchRepository branchRepo;
    private final QuizAttemptRepository quizAttemptRepo;
    private final MaterialProgressRepository materialProgressRepo;
    private final ChatMessageRepository chatRepo;
    private final LabRepository labRepo;
    private final LabSubmissionRepository labSubmissionRepo;
    private final ExamAttemptRepository examAttemptRepo;
    private final ExamRepository examRepo;
    private final DockerLabService dockerLabService;
    private final LabActivityRepository labActivityRepo;

    public AdminController(UserRepository userRepo, CourseRepository courseRepo,
                           EnrollmentRepository enrollmentRepo, AssignmentRepository assignmentRepo,
                           QuizRepository quizRepo, QuizQuestionRepository questionRepo,
                           NotificationRepository notificationRepo, PlatformSettingsRepository settingsRepo,
                           CourseMaterialRepository materialRepo,
                           AssignmentSubmissionRepository submissionRepo,
                           LabVmRepository labVmRepo, FileStorage fileStorage,
                           BranchRepository branchRepo, QuizAttemptRepository quizAttemptRepo,
                           MaterialProgressRepository materialProgressRepo,
                           ChatMessageRepository chatRepo,
                           LabRepository labRepo, LabSubmissionRepository labSubmissionRepo,
                           ExamAttemptRepository examAttemptRepo, ExamRepository examRepo, DockerLabService dockerLabService, LabActivityRepository labActivityRepo) {
        this.userRepo = userRepo;
        this.courseRepo = courseRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.assignmentRepo = assignmentRepo;
        this.quizRepo = quizRepo;
        this.questionRepo = questionRepo;
        this.notificationRepo = notificationRepo;
        this.settingsRepo = settingsRepo;
        this.materialRepo = materialRepo;
        this.submissionRepo = submissionRepo;
        this.labVmRepo = labVmRepo;
        this.fileStorage = fileStorage;
        this.branchRepo = branchRepo;
        this.quizAttemptRepo = quizAttemptRepo;
        this.materialProgressRepo = materialProgressRepo;
        this.chatRepo = chatRepo;
        this.labRepo = labRepo;
        this.labSubmissionRepo = labSubmissionRepo;
        this.examAttemptRepo = examAttemptRepo;
        this.examRepo = examRepo;
        this.dockerLabService = dockerLabService;
        this.labActivityRepo = labActivityRepo;
    }

    private User requireAdmin(HttpSession session) {
        User u = (User) session.getAttribute("adminUser");
        if (u == null || !"ADMIN".equals(u.getRole())) {
            return null;
        }
        return u;
    }

    // ========== DASHBOARD ==========
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";

        List<User> allStudents = userRepo.findByRole("STUDENT").stream()
                .filter(u -> !"Deleted".equals(u.getStatus()))
                .collect(Collectors.toList());
        long totalStudents = allStudents.size();
        long totalCourses = courseRepo.count();
        long activeCourses = courseRepo.findAll().stream().filter(c -> "Published".equals(c.getStatus())).count();
        long enrollments = enrollmentRepo.count();

        // Pending assignment submissions: published assignments with students who have not submitted
        List<Assignment> publishedAssignments = assignmentRepo.findAll().stream()
                .filter(a -> "Published".equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.toList());
        Set<String> submittedPairs = submissionRepo.findAll().stream()
                .map(s -> s.getAssignmentId() + ":" + s.getStudentId())
                .collect(Collectors.toSet());
        long pendingSubmissions = 0;
        List<Map<String, Object>> pendingList = new ArrayList<>();
        for (Assignment a : publishedAssignments) {
            for (User st : allStudents) {
                String br = st.getBranch() != null ? st.getBranch() : st.getDepartment();
                if (a.getBranch() != null && !a.getBranch().isBlank()
                        && br != null && !a.getBranch().equalsIgnoreCase(br)
                        && !"All".equalsIgnoreCase(a.getBranch())) {
                    continue;
                }
                if (!submittedPairs.contains(a.getId() + ":" + st.getId())) {
                    pendingSubmissions++;
                    if (pendingList.size() < 15) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("studentName", st.getName());
                        row.put("assignmentTitle", a.getTitle());
                        row.put("branch", br != null ? br : "—");
                        row.put("dueDate", a.getDueDate());
                        pendingList.add(row);
                    }
                }
            }
        }

        // Monthly enrollments (last 6 months labels + counts)
        LocalDateTime now = LocalDateTime.now();
        List<String> monthLabels = new ArrayList<>();
        List<Long> monthCounts = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end = start.plusMonths(1);
            String label = start.getMonth().name().substring(0, 3) + " " + start.getYear();
            long c = enrollmentRepo.findAll().stream()
                    .filter(e -> e.getEnrolledAt() != null
                            && !e.getEnrolledAt().isBefore(start)
                            && e.getEnrolledAt().isBefore(end))
                    .count();
            monthLabels.add(label);
            monthCounts.add(c);
        }

        // Engagement by course
        List<Map<String, Object>> courseEngagement = new ArrayList<>();
        for (Course c : courseRepo.findAll()) {
            if (!"Published".equalsIgnoreCase(c.getStatus())) continue;
            long enr = enrollmentRepo.findAll().stream().filter(e -> c.getId().equals(e.getCourseId())).count();
            double avgProg = enrollmentRepo.findAll().stream()
                    .filter(e -> c.getId().equals(e.getCourseId()))
                    .mapToInt(Enrollment::getProgress).average().orElse(0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("title", c.getTitle());
            row.put("enrollments", enr);
            row.put("completion", Math.round(avgProg));
            courseEngagement.add(row);
        }
        courseEngagement.sort((a, b) -> Long.compare((Long) b.get("enrollments"), (Long) a.get("enrollments")));

        long totalAssignments = assignmentRepo.count();
        long totalSubmissions = submissionRepo.count();
        long quizAttempts = quizAttemptRepo.count();
        long totalQuizzes = quizRepo.count();

        List<User> recentUsers = allStudents.stream()
                .sorted(Comparator.comparing(User::getJoinedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .collect(Collectors.toList());

        model.addAttribute("admin", admin);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalCourses", totalCourses);
        model.addAttribute("activeCourses", activeCourses);
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("pendingSubmissions", pendingSubmissions);
        model.addAttribute("pendingList", pendingList);
        model.addAttribute("monthLabels", monthLabels);
        model.addAttribute("monthCounts", monthCounts);
        model.addAttribute("courseEngagement", courseEngagement);
        model.addAttribute("totalAssignments", totalAssignments);
        model.addAttribute("totalSubmissions", totalSubmissions);
        model.addAttribute("quizAttempts", quizAttempts);
        model.addAttribute("totalQuizzes", totalQuizzes);
        model.addAttribute("recentUsers", recentUsers);
        model.addAttribute("activePage", "dashboard");
        return "admin/dashboard";
    }

    // ========== USERS ==========
    @GetMapping("/users")
    public String users(@RequestParam(required = false, defaultValue = "all") String filter,
                        @RequestParam(required = false) String branch,
                        HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";

        List<User> users = userRepo.findByRole("STUDENT");
        if (branch != null && !branch.isBlank()) {
            users = users.stream()
                    .filter(u -> branch.equalsIgnoreCase(u.getBranch())
                            || branch.equalsIgnoreCase(u.getDepartment()))
                    .collect(Collectors.toList());
        }
        model.addAttribute("admin", admin);
        model.addAttribute("users", users);
        model.addAttribute("filter", filter);
        model.addAttribute("branch", branch);
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("activePage", "users");
        return "admin/users";
    }

    // ========== STUDENTS ==========
    @GetMapping("/students")
    public String students(@RequestParam(required = false) String department,
                           @RequestParam(required = false) String batch,
                           HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";

        List<User> students = userRepo.findByRole("STUDENT");
        if (department != null && !department.isBlank() && !"All".equals(department)) {
            students = students.stream().filter(s -> department.equals(s.getDepartment())).collect(Collectors.toList());
        }
        if (batch != null && !batch.isBlank() && !"All".equals(batch)) {
            students = students.stream().filter(s -> batch.equals(s.getBatch())).collect(Collectors.toList());
        }

        Map<Long, Integer> progressMap = new HashMap<>();
        Map<Long, String> gradeMap = new HashMap<>();
        for (User s : students) {
            List<QuizAttempt> attempts = quizAttemptRepo.findByStudentId(s.getId());
            if (attempts.isEmpty()) {
                progressMap.put(s.getId(), 0);
                gradeMap.put(s.getId(), "—");
            } else {
                double avg = attempts.stream().mapToDouble(QuizAttempt::getPercent).average().orElse(0);
                progressMap.put(s.getId(), (int) Math.round(avg));
                gradeMap.put(s.getId(), GradeUtil.letter(avg));
            }
        }

        model.addAttribute("admin", admin);
        model.addAttribute("students", students);
        model.addAttribute("progressMap", progressMap);
        model.addAttribute("gradeMap", gradeMap);
        model.addAttribute("activePage", "students");
        return "admin/students";
    }

    // ========== COURSES ==========
    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) String branch,
                          HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        List<Course> courses = courseRepo.findAll();
        if (branch != null && !branch.isBlank()) {
            courses = courses.stream()
                    .filter(c -> branch.equalsIgnoreCase(c.getCategory()))
                    .collect(Collectors.toList());
        }
        model.addAttribute("admin", admin);
        model.addAttribute("courses", courses);
        model.addAttribute("branch", branch);
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("activePage", "courses");
        return "admin/courses";
    }

    @GetMapping("/courses/create")
    public String createCourseForm(HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        model.addAttribute("admin", admin);
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("activePage", "courses");
        return "admin/course-create";
    }

    @PostMapping("/courses/create")
    public String createCourse(@RequestParam String title,
                               @RequestParam String branch,
                               @RequestParam(required = false) String instructorName,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) MultipartFile[] videos,
                               @RequestParam(required = false) MultipartFile[] pdfs,
                               @RequestParam(required = false) MultipartFile[] notes,
                               @RequestParam(required = false) MultipartFile coverFile,
                               HttpSession session) throws IOException {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";

        String iname = (instructorName != null && !instructorName.isBlank()) ? instructorName : admin.getName();
        Course course = new Course(title, branch, iname, status != null ? status : "Published");
        course.setInstructorId(admin.getId());
        course.setDescription(description);
        if (coverFile != null && !coverFile.isEmpty()) {
            course.setCoverImage(fileStorage.store(coverFile, "covers"));
        }
        course = courseRepo.save(course);

        saveMaterials(course, videos, "VIDEO", "videos");
        saveMaterials(course, pdfs, "PDF", "pdfs");
        saveMaterials(course, notes, "NOTE", "notes");

        return "redirect:/admin/courses";
    }

    private void saveMaterials(Course course, MultipartFile[] files, String type, String folder) throws IOException {
        if (files == null) return;
        int idx = materialRepo.findAll().stream()
                .filter(x -> x.getCourse() != null && x.getCourse().getId().equals(course.getId()))
                .mapToInt(CourseMaterial::getOrderIndex)
                .max().orElse(-1) + 1;
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String webPath = fileStorage.store(file, folder);
                CourseMaterial m = new CourseMaterial();
                m.setTitle(file.getOriginalFilename());
                m.setType(type);
                m.setFilePath(webPath);
                m.setOrderIndex(idx++);
                m.setCourse(course);
                materialRepo.save(m);
            }
        }
    }

    @GetMapping("/courses/{id}/edit")
    public String editCourse(@PathVariable Long id, HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        Course course = courseRepo.findById(id).orElse(null);
        if (course == null) return "redirect:/admin/courses";
        model.addAttribute("admin", admin);
        model.addAttribute("course", course);
        model.addAttribute("materials", materialRepo.findAll().stream()
                .filter(m -> m.getCourse() != null && m.getCourse().getId().equals(id))
                .sorted(Comparator.comparingInt(CourseMaterial::getOrderIndex))
                .collect(Collectors.toList()));
        model.addAttribute("activePage", "courses");
        return "admin/course-edit";
    }

    @PostMapping("/courses/{id}/edit")
    public String updateCourse(@PathVariable Long id,
                               @RequestParam String title,
                               @RequestParam String category,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String status,
                               HttpSession session) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        Course course = courseRepo.findById(id).orElse(null);
        if (course != null) {
            course.setTitle(title);
            course.setCategory(category);
            course.setDescription(description);
            if (status != null) course.setStatus(status);
            courseRepo.save(course);
        }
        return "redirect:/admin/courses";
    }

    @PostMapping("/materials/{id}/update-note")
    public String updateNote(@PathVariable Long id, @RequestParam String content, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        CourseMaterial m = materialRepo.findById(id).orElse(null);
        if (m != null && "NOTE".equals(m.getType())) {
            m.setContent(content);
            materialRepo.save(m);
            return "redirect:/admin/courses/" + m.getCourse().getId() + "/edit";
        }
        return "redirect:/admin/courses";
    }


    @GetMapping("/courses/{id}/view")
    public String viewCourseAdmin(@PathVariable Long id, HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        Course course = courseRepo.findById(id).orElse(null);
        if (course == null) return "redirect:/admin/courses";
        model.addAttribute("admin", admin);
        model.addAttribute("course", course);
        model.addAttribute("materials", materialRepo.findAll().stream()
                .filter(m -> m.getCourse() != null && m.getCourse().getId().equals(id))
                .sorted(Comparator.comparingInt(CourseMaterial::getOrderIndex))
                .collect(Collectors.toList()));
        model.addAttribute("activePage", "courses");
        return "admin/course-view";
    }

    // ========== ENROLLMENTS ==========
    @GetMapping("/enrollments")
    public String enrollments(@RequestParam(required = false) String branch,
                              HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        List<Enrollment> list = enrollmentRepo.findAll();
        if (branch != null && !branch.isBlank()) {
            Set<Long> ids = userRepo.findByRole("STUDENT").stream()
                    .filter(u -> branch.equalsIgnoreCase(u.getBranch()) || branch.equalsIgnoreCase(u.getDepartment()))
                    .map(User::getId).collect(Collectors.toSet());
            list = list.stream().filter(e -> ids.contains(e.getStudentId())).collect(Collectors.toList());
        }
        model.addAttribute("admin", admin);
        model.addAttribute("enrollments", list);
        model.addAttribute("branch", branch);
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("activePage", "enrollments");
        return "admin/enrollments";
    }

    // ========== ASSESSMENTS ==========
    @GetMapping("/assessments")
    public String assessments(@RequestParam(required = false, defaultValue = "assignments") String tab,
                              HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";

        model.addAttribute("admin", admin);
        model.addAttribute("assignments", assignmentRepo.findAll());
        List<Quiz> quizzes = quizRepo.findAll();
        model.addAttribute("quizzes", quizzes);
        Map<Long, Long> qCount = new HashMap<>();
        for (Quiz qz : quizzes) {
            long c = questionRepo.findAll().stream()
                    .filter(qq -> qq.getQuiz() != null && qq.getQuiz().getId().equals(qz.getId()))
                    .count();
            qCount.put(qz.getId(), c);
        }
        model.addAttribute("qCount", qCount);
        model.addAttribute("exams", examRepo.findAll());
        model.addAttribute("tab", tab);
        model.addAttribute("activePage", "assessments");
        return "admin/assessments";
    }

    @GetMapping("/assessments/assignment/create")
    public String createAssignmentForm(HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        model.addAttribute("admin", admin);
        model.addAttribute("courses", courseRepo.findAll());
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("activePage", "assessments");
        return "admin/assignment-create";
    }

    @PostMapping("/assessments/assignment/create")
    public String createAssignment(@RequestParam String title,
                                   @RequestParam Long courseId,
                                   @RequestParam String dueDate,
                                   @RequestParam(required = false) String branch,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(required = false) MultipartFile file,
                                   @RequestParam(required = false, defaultValue = "false") boolean dailyTask,
                                   @RequestParam(required = false, defaultValue = "3") int dailyCount,
                                   @RequestParam(required = false) String taskInstructions,
                                   HttpSession session) throws IOException {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        Course course = courseRepo.findById(courseId).orElse(null);
        Assignment a = new Assignment();
        a.setTitle(title);
        a.setCourseId(courseId);
        a.setCourseTitle(course != null ? course.getTitle() : "");
        a.setInstructorId(admin.getId());
        a.setInstructorName(admin.getName());
        a.setDueDate(LocalDate.parse(dueDate));
        a.setDescription(description);
        a.setStatus("Published");
        a.setBranch(branch != null ? branch : (course != null ? course.getCategory() : null));
        a.setDailyTask(false);
        a.setDailyCount(0);
        a.setTaskInstructions(null);
        a.setSubmissions(0);
        a.setTotalStudents(course != null ? course.getEnrolledCount() : 0);
        a.setCreatedAt(LocalDateTime.now());
        if (file != null && !file.isEmpty()) {
            a.setFilePath(fileStorage.store(file, "assignments"));
        }
        assignmentRepo.save(a);
        return "redirect:/admin/assessments?tab=assignments";
    }

    @GetMapping("/assessments/quiz/create")
    public String createQuizForm(HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        model.addAttribute("admin", admin);
        model.addAttribute("courses", courseRepo.findAll());
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("activePage", "assessments");
        return "admin/quiz-create";
    }

    @PostMapping("/assessments/quiz/create")
    public String createQuiz(@RequestParam String title,
                             @RequestParam(required = false) String category,
                             @RequestParam(required = false) String branch,
                             @RequestParam(required = false) String courseName,
                             @RequestParam(required = false) Long courseId,
                             @RequestParam(required = false) String mode,
                             @RequestParam(required = false) List<String> questionText,
                             @RequestParam(required = false) List<String> qText,
                             @RequestParam(required = false) List<String> optionA,
                             @RequestParam(required = false) List<String> qA,
                             @RequestParam(required = false) List<String> optionB,
                             @RequestParam(required = false) List<String> qB,
                             @RequestParam(required = false) List<String> optionC,
                             @RequestParam(required = false) List<String> qC,
                             @RequestParam(required = false) List<String> optionD,
                             @RequestParam(required = false) List<String> qD,
                             @RequestParam(required = false) List<String> correctOption,
                             @RequestParam(required = false) List<String> qAns,
                             @RequestParam(required = false) MultipartFile quizFile,
                             @RequestParam(required = false) MultipartFile questionFile,
                             @RequestParam(required = false) String pasteContent,
                             @RequestParam(required = false, defaultValue = "0") int questionLimit,
                             HttpSession session) throws Exception {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";

        String cat = branch != null && !branch.isBlank() ? branch : (category != null ? category : "All");
        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setCategory(cat);
        quiz.setCourseId(courseId);
        quiz.setCourseTitle(courseName);
        if (courseId != null) {
            Course cc = courseRepo.findById(courseId).orElse(null);
            if (cc != null) quiz.setCourseTitle(cc.getTitle());
        }
        quiz.setInstructorId(admin.getId());
        quiz.setStatus("Published");
        quiz.setShuffle(true);
        quiz.setMaxAttempts(2);
        quiz.setCreatedAt(LocalDateTime.now());
        quiz = quizRepo.save(quiz);

        List<String> texts = qText != null ? qText : questionText;
        List<String> oa = qA != null ? qA : optionA;
        List<String> ob = qB != null ? qB : optionB;
        List<String> oc = qC != null ? qC : optionC;
        List<String> od = qD != null ? qD : optionD;
        List<String> ans = qAns != null ? qAns : correctOption;

        if (texts != null && !texts.isEmpty()) {
            for (int i = 0; i < texts.size(); i++) {
                if (texts.get(i) == null || texts.get(i).isBlank()) continue;
                QuizQuestion q = new QuizQuestion();
                q.setQuestionText(texts.get(i));
                q.setOptionA(oa != null && i < oa.size() ? oa.get(i) : "Option A");
                q.setOptionB(ob != null && i < ob.size() ? ob.get(i) : "Option B");
                q.setOptionC(oc != null && i < oc.size() ? oc.get(i) : "Option C");
                q.setOptionD(od != null && i < od.size() ? od.get(i) : "Option D");
                q.setCorrectOption(ans != null && i < ans.size() ? ans.get(i) : "A");
                q.setOrderIndex(i);
                q.setQuiz(quiz);
                questionRepo.save(q);
            }
        }

        MultipartFile file = questionFile != null && !questionFile.isEmpty() ? questionFile : quizFile;
        if (file != null && !file.isEmpty()) {
            try {
                String content = QuizTextParser.extractText(file);
                importParseQuiz(quiz, content, questionLimit);
            } catch (Exception ex) {
                try {
                    importParseQuiz(quiz, new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8), questionLimit);
                } catch (Exception ignored) {}
            }
        } else if (pasteContent != null && !pasteContent.isBlank()) {
            importParseQuiz(quiz, pasteContent, questionLimit);
        }
        quiz.setStatus("Published");
        if (quiz.getMaxAttempts() <= 0) quiz.setMaxAttempts(2);
        quizRepo.save(quiz);
        return "redirect:/admin/assessments?tab=quizzes";
    }

    
    private void importParseQuiz(Quiz quiz, String content, int questionLimit) {
        List<QuizTextParser.ParsedQ> all = new ArrayList<>(QuizTextParser.parse(content));
        if (questionLimit > 0 && questionLimit < all.size()) {
            Collections.shuffle(all);
            all = new ArrayList<>(all.subList(0, questionLimit));
        }
        int idx = 0;
        for (QuizTextParser.ParsedQ pq : all) {
            QuizQuestion q = new QuizQuestion();
            q.setQuestionText(pq.text);
            q.setOptionA(pq.a); q.setOptionB(pq.b); q.setOptionC(pq.c); q.setOptionD(pq.d);
            q.setCorrectOption(pq.ans);
            q.setOrderIndex(idx++);
            q.setQuiz(quiz);
            questionRepo.save(q);
        }
        quiz.setShuffle(true);
        quizRepo.save(quiz);
    }

    
    // ========== REPORTS ==========
    @GetMapping("/reports")
    public String reports(@RequestParam(required = false, defaultValue = "student") String type,
                          @RequestParam(required = false) String branch,
                          HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";

        List<User> students = userRepo.findByRole("STUDENT").stream()
                .filter(u -> !"Deleted".equals(u.getStatus()))
                .collect(Collectors.toList());
        List<Course> courses = courseRepo.findAll();
        List<Enrollment> enrollments = enrollmentRepo.findAll();
        List<Assignment> assignments = assignmentRepo.findAll();
        List<QuizAttempt> attempts = quizAttemptRepo.findAll();

        if (branch != null && !branch.isBlank()) {
            students = students.stream()
                    .filter(u -> branch.equalsIgnoreCase(u.getBranch()) || branch.equalsIgnoreCase(u.getDepartment()))
                    .collect(Collectors.toList());
            Set<Long> stuIds = students.stream().map(User::getId).collect(Collectors.toSet());
            courses = courses.stream()
                    .filter(c -> branch.equalsIgnoreCase(c.getCategory()))
                    .collect(Collectors.toList());
            enrollments = enrollments.stream()
                    .filter(e -> stuIds.contains(e.getStudentId()))
                    .collect(Collectors.toList());
            assignments = assignments.stream()
                    .filter(a -> branch.equalsIgnoreCase(a.getBranch()) || "All".equalsIgnoreCase(a.getBranch()))
                    .collect(Collectors.toList());
            attempts = attempts.stream()
                    .filter(a -> stuIds.contains(a.getStudentId()))
                    .collect(Collectors.toList());
        }

        // published exam attempts only for grade report
        java.util.Set<Long> publishedExamIds = examRepo.findAll().stream()
                .filter(Exam::isResultsPublished)
                .map(Exam::getId)
                .collect(Collectors.toSet());
        List<ExamAttempt> examAttempts = examAttemptRepo.findAll().stream()
                .filter(a -> publishedExamIds.contains(a.getExamId()))
                .filter(a -> a.getTotal() > 0)
                .collect(Collectors.toList());
        if (branch != null && !branch.isBlank()) {
            examAttempts = examAttempts.stream()
                    .filter(a -> branch.equalsIgnoreCase(a.getBranch()))
                    .collect(Collectors.toList());
        }
        Map<Long, String> rollMap = new HashMap<>();
        Map<Long, String> examRollMap = new HashMap<>();
        for (User u : userRepo.findByRole("STUDENT")) {
            String roll = u.getRollNo() != null ? u.getRollNo() : "—";
            rollMap.put(u.getId(), roll);
            examRollMap.put(u.getId(), roll);
        }

        model.addAttribute("admin", admin);
        model.addAttribute("type", type);
        model.addAttribute("branch", branch);
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("students", students);
        model.addAttribute("courses", courses);
        model.addAttribute("attempts", attempts);
        model.addAttribute("examAttempts", examAttempts);
        model.addAttribute("rollMap", rollMap);
        model.addAttribute("examRollMap", examRollMap);
        model.addAttribute("activePage", "reports");
        return "admin/reports";
    }

    // ========== NOTIFICATIONS ==========
    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        model.addAttribute("admin", admin);
        model.addAttribute("notifications", notificationRepo.findAll());
        model.addAttribute("activePage", "notifications");
        return "admin/notifications";
    }

    @PostMapping("/notifications/send")
    public String sendNotification(@RequestParam String title,
                                   @RequestParam String message,
                                   @RequestParam String audience,
                                   HttpSession session) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        Notification n = new Notification();
        n.setTitle(title);
        n.setMessage(message);
        n.setAudience(audience);
        n.setSentAt(LocalDateTime.now());
        n.setSentBy(admin.getId());
        notificationRepo.save(n);
        return "redirect:/admin/notifications";
    }

    // ========== SETTINGS ==========
    @GetMapping("/settings")
    public String settings(HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        PlatformSettings settings = settingsRepo.findAll().stream().findFirst().orElseGet(() -> settingsRepo.save(new PlatformSettings()));
        model.addAttribute("admin", admin);
        model.addAttribute("settings", settings);
        model.addAttribute("activePage", "profile");
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam(required = false) String allowSelfRegistration,
                               @RequestParam(required = false) String requireEnrollmentApproval,
                               @RequestParam(required = false) String studentPortalEnabled,
                               HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        PlatformSettings s = settingsRepo.findAll().stream().findFirst().orElseGet(() -> new PlatformSettings());
        s.setAllowSelfRegistration(allowSelfRegistration != null);
        s.setRequireEnrollmentApproval(requireEnrollmentApproval != null);
        s.setStudentPortalEnabled(studentPortalEnabled != null);
        settingsRepo.save(s);
        return "redirect:/admin/settings?saved=1";
    }

    @PostMapping("/settings/profile")
    public String saveAdminProfile(@RequestParam String name,
                                   @RequestParam String email,
                                   @RequestParam(required = false) String mobile,
                                   HttpSession session) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        if (mobile != null && !mobile.isBlank() && !mobile.matches("\\d{10}")) {
            return "redirect:/admin/settings?error=Mobile+must+be+10+digits";
        }
        User u = userRepo.findById(admin.getId()).orElse(admin);
        u.setName(name);
        var existing = userRepo.findByEmail(email);
        if (existing.isPresent() && !existing.get().getId().equals(u.getId())) {
            return "redirect:/admin/settings?error=Email+already+used";
        }
        u.setEmail(email);
        u.setMobile(mobile);
        userRepo.save(u);
        session.setAttribute("adminUser", u);
        return "redirect:/admin/settings?saved=1";
    }

    // ========== SUBMISSIONS (instructor sees student work) ==========
    @GetMapping("/submissions")
    public String submissions(@RequestParam(required = false) String branch,
                              HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        List<AssignmentSubmission> list;
        if ("ADMIN".equals(admin.getRole())) {
            list = submissionRepo.findAll();
        } else {
            list = submissionRepo.findByInstructorId(admin.getId());
        }
        if (branch != null && !branch.isBlank()) {
            Set<Long> ids = userRepo.findByRole("STUDENT").stream()
                    .filter(u -> branch.equalsIgnoreCase(u.getBranch()) || branch.equalsIgnoreCase(u.getDepartment()))
                    .map(User::getId).collect(Collectors.toSet());
            list = list.stream().filter(s -> ids.contains(s.getStudentId())).collect(Collectors.toList());
        }
        model.addAttribute("admin", admin);
        model.addAttribute("submissions", list);
        model.addAttribute("branch", branch);
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("activePage", "submissions");
        return "admin/submissions";
    }

    // ========== LAB VM ==========
    @GetMapping("/lab")
    public String labPage(HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        model.addAttribute("admin", admin);
        model.addAttribute("vms", labVmRepo.findAll());
        model.addAttribute("students", userRepo.findByRole("STUDENT"));
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("activePage", "lab");
        return "admin/lab";
    }

    @PostMapping("/lab/create")
    public String createLab(@RequestParam String name,
                            @RequestParam(defaultValue = "Ubuntu") String osType,
                            @RequestParam(required = false) String description,
                            @RequestParam(required = false) String branch,
                            @RequestParam(required = false) Long studentId,
                            HttpSession session) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";

        List<User> targets = new ArrayList<>();
        if (studentId != null) {
            userRepo.findById(studentId).ifPresent(targets::add);
        } else if (branch != null && !branch.isBlank()) {
            targets = userRepo.findByRole("STUDENT").stream()
                    .filter(u -> branch.equalsIgnoreCase(u.getBranch()) || branch.equalsIgnoreCase(u.getDepartment()))
                    .collect(Collectors.toList());
        } else {
            // No branch / student selected → all registered students
            targets = userRepo.findByRole("STUDENT").stream()
                    .filter(u -> !"Deleted".equals(u.getStatus()))
                    .collect(Collectors.toList());
        }

        if (targets.isEmpty()) {
            // Still create one unassigned lab slot
            LabVm vm = new LabVm();
            vm.setName(name);
            vm.setOsType(osType);
            vm.setDescription(description);
            vm.setCreatedByAdminId(admin.getId());
            vm.setCreatedAt(LocalDateTime.now());
            vm.setStatus("Available");
            vm = labVmRepo.save(vm);
            vm.setAccessUrl("/student/lab/" + vm.getId());
            labVmRepo.save(vm);
            return "redirect:/admin/lab";
        }

        for (User stu : targets) {
            LabVm vm = new LabVm();
            vm.setName(name + " — " + stu.getName());
            vm.setOsType(osType);
            vm.setDescription(description);
            vm.setCreatedByAdminId(admin.getId());
            vm.setCreatedAt(LocalDateTime.now());
            vm.setAssignedStudentId(stu.getId());
            vm.setAssignedStudentName(stu.getName());
            vm.setBranch(stu.getBranch() != null ? stu.getBranch() : stu.getDepartment());
            vm.setStatus("Assigned");
            vm = labVmRepo.save(vm);
            vm.setAccessUrl("/student/lab/" + vm.getId());
            labVmRepo.save(vm);
        }
        return "redirect:/admin/lab";
    }

    @PostMapping("/lab/{id}/assign")
    public String assignLab(@PathVariable Long id, @RequestParam Long studentId, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        LabVm vm = labVmRepo.findById(id).orElse(null);
        if (vm != null) {
            User stu = userRepo.findById(studentId).orElse(null);
            if (stu != null) {
                vm.setAssignedStudentId(stu.getId());
                vm.setAssignedStudentName(stu.getName());
                vm.setStatus("Assigned");
                vm.setAccessUrl("/student/lab/" + vm.getId());
                labVmRepo.save(vm);
            }
        }
        return "redirect:/admin/lab";
    }


    @PostMapping("/courses/{id}/deactivate")
    public String deactivateCourse(@PathVariable Long id, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        courseRepo.findById(id).ifPresent(c -> {
            c.setStatus("Inactive");
            c.setPublished(false);
            courseRepo.save(c);
        });
        return "redirect:/admin/courses";
    }

    @PostMapping("/courses/{id}/activate")
    public String activateCourse(@PathVariable Long id, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        courseRepo.findById(id).ifPresent(c -> {
            c.setStatus("Published");
            c.setPublished(true);
            courseRepo.save(c);
        });
        return "redirect:/admin/courses";
    }

    @PostMapping("/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        materialRepo.findAll().stream()
                .filter(m -> m.getCourse() != null && id.equals(m.getCourse().getId()))
                .forEach(materialRepo::delete);
        courseRepo.deleteById(id);
        return "redirect:/admin/courses";
    }

    @PostMapping("/materials/{id}/delete")
    public String deleteMaterial(@PathVariable Long id, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        CourseMaterial m = materialRepo.findById(id).orElse(null);
        Long courseId = m != null && m.getCourse() != null ? m.getCourse().getId() : null;
        if (m != null) materialRepo.delete(m);
        return courseId != null ? "redirect:/admin/courses/" + courseId + "/edit" : "redirect:/admin/courses";
    }

    @PostMapping("/assessments/assignment/{id}/delete")
    public String deleteAssignment(@PathVariable Long id, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        assignmentRepo.deleteById(id);
        return "redirect:/admin/assessments?tab=assignments";
    }

    // ========== BRANCHES ==========
    @GetMapping("/branches")
    public String branches(HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        model.addAttribute("admin", admin);
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("activePage", "branches");
        return "admin/branches";
    }

    @PostMapping("/branches/add")
    public String addBranch(@RequestParam String code, @RequestParam String name, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        if (code != null && !code.isBlank() && branchRepo.findByCode(code.trim().toUpperCase()).isEmpty()) {
            branchRepo.save(new Branch(code.trim().toUpperCase(), name != null ? name : code));
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/branches/{id}/delete")
    public String deleteBranch(@PathVariable Long id, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        branchRepo.findById(id).ifPresent(branchRepo::delete);
        return "redirect:/admin/branches";
    }

    // ========== MATERIAL ORDER / EXTRA UPLOAD ==========
    @PostMapping("/courses/{courseId}/materials/upload")
    public String uploadMoreMaterials(@PathVariable Long courseId,
                                      @RequestParam(required = false) MultipartFile[] videos,
                                      @RequestParam(required = false) MultipartFile[] pdfs,
                                      @RequestParam(required = false) MultipartFile[] notes,
                                      HttpSession session) throws IOException {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        Course course = courseRepo.findById(courseId).orElse(null);
        if (course != null) {
            saveMaterials(course, videos, "VIDEO", "videos");
            saveMaterials(course, pdfs, "PDF", "pdfs");
            saveMaterials(course, notes, "NOTE", "notes");
        }
        return "redirect:/admin/courses/" + courseId + "/edit";
    }

    @PostMapping("/materials/{id}/move")
    public String moveMaterial(@PathVariable Long id, @RequestParam String dir, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        CourseMaterial m = materialRepo.findById(id).orElse(null);
        if (m == null || m.getCourse() == null) return "redirect:/admin/courses";
        Long courseId = m.getCourse().getId();
        List<CourseMaterial> list = materialRepo.findAll().stream()
                .filter(x -> x.getCourse() != null && x.getCourse().getId().equals(courseId))
                .sorted(Comparator.comparingInt(CourseMaterial::getOrderIndex))
                .collect(Collectors.toList());
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) { idx = i; break; }
        }
        if (idx < 0) return "redirect:/admin/courses/" + courseId + "/edit";
        int swap = "up".equals(dir) ? idx - 1 : idx + 1;
        if (swap < 0 || swap >= list.size()) return "redirect:/admin/courses/" + courseId + "/edit";
        CourseMaterial a = list.get(idx);
        CourseMaterial b = list.get(swap);
        int tmp = a.getOrderIndex();
        a.setOrderIndex(b.getOrderIndex());
        b.setOrderIndex(tmp);
        materialRepo.save(a);
        materialRepo.save(b);
        return "redirect:/admin/courses/" + courseId + "/edit";
    }

    @PostMapping("/quiz/{id}/extra-attempts")
    public String grantExtraAttempts(@PathVariable Long id, @RequestParam(defaultValue = "1") int extra, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        Quiz quiz = quizRepo.findById(id).orElse(null);
        if (quiz != null) {
            quiz.setMaxAttempts(quiz.getMaxAttempts() + Math.max(1, extra));
            quizRepo.save(quiz);
        }
        return "redirect:/admin/assessments?tab=quizzes";
    }

    @GetMapping("/reports/branch")
    public String branchReports(@RequestParam(required = false) String branch,
                                @RequestParam(required = false, defaultValue = "grade") String type,
                                HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        model.addAttribute("admin", admin);
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("branch", branch);
        model.addAttribute("type", type);
        model.addAttribute("activePage", "reports");
        if (branch != null && !branch.isBlank()) {
            List<User> branchStudents = userRepo.findByRole("STUDENT").stream()
                    .filter(s -> branch.equalsIgnoreCase(s.getBranch()) || branch.equalsIgnoreCase(s.getDepartment()))
                    .collect(Collectors.toList());
            Set<Long> studentIds = branchStudents.stream().map(User::getId).collect(Collectors.toSet());
            List<Course> branchCourses = courseRepo.findAll().stream()
                    .filter(c -> branch.equalsIgnoreCase(c.getCategory())).collect(Collectors.toList());
            Set<Long> courseIds = branchCourses.stream().map(Course::getId).collect(Collectors.toSet());
            model.addAttribute("students", branchStudents);
            model.addAttribute("courses", branchCourses);
            model.addAttribute("quizzes", quizRepo.findAll().stream()
                    .filter(q -> branch.equalsIgnoreCase(q.getCategory()) || "All".equalsIgnoreCase(q.getCategory()))
                    .collect(Collectors.toList()));
            model.addAttribute("assignments", assignmentRepo.findAll().stream()
                    .filter(a -> branch.equalsIgnoreCase(a.getBranch())).collect(Collectors.toList()));
            model.addAttribute("attempts", quizAttemptRepo.findAll().stream()
                    .filter(a -> branch.equalsIgnoreCase(a.getBranch()) || studentIds.contains(a.getStudentId()))
                    .collect(Collectors.toList()));
            model.addAttribute("enrollments", enrollmentRepo.findAll().stream()
                    .filter(e -> studentIds.contains(e.getStudentId()) || courseIds.contains(e.getCourseId()))
                    .collect(Collectors.toList()));
        }
        return "admin/reports-branch";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteStudent(@PathVariable Long id, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        userRepo.findById(id).ifPresent(u -> {
            if ("STUDENT".equals(u.getRole())) {
                u.setStatus("Deleted");
                u.setPassword("DELETED_" + System.currentTimeMillis());
                userRepo.save(u);
            }
        });
        return "redirect:/admin/users?filter=students";
    }

    @PostMapping("/users/delete-all-students")
    public String deleteAllStudents(HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        for (User u : userRepo.findByRole("STUDENT")) {
            u.setStatus("Deleted");
            u.setPassword("DELETED_" + System.currentTimeMillis());
            userRepo.save(u);
        }
        return "redirect:/admin/users?filter=students";
    }

    @GetMapping("/chat")
    public String adminChat(HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        List<ChatMessage> messages = chatRepo.findAllByOrderBySentAtDesc();
        Map<Long, String> rollMap = new HashMap<>();
        for (User u : userRepo.findByRole("STUDENT")) {
            rollMap.put(u.getId(), u.getRollNo() != null ? u.getRollNo() : "—");
        }
        model.addAttribute("admin", admin);
        model.addAttribute("messages", messages);
        model.addAttribute("rollMap", rollMap);
        model.addAttribute("activePage", "chat");
        return "admin/chat";
    }

    @PostMapping("/chat/reply")
    public String chatReply(@RequestParam String message, @RequestParam Long toUserId, HttpSession session) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        ChatMessage m = new ChatMessage();
        m.setFromUserId(admin.getId());
        m.setFromName(admin.getName());
        m.setFromRole("ADMIN");
        m.setToUserId(toUserId);
        m.setMessage(message);
        m.setReadFlag(false);
        m.setSentAt(java.time.LocalDateTime.now());
        chatRepo.save(m);
        return "redirect:/admin/chat";
    }

    @PostMapping("/assessments/quiz/{id}/delete")
    public String deleteQuiz(@PathVariable Long id, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        questionRepo.findAll().stream()
                .filter(q -> q.getQuiz() != null && id.equals(q.getQuiz().getId()))
                .forEach(questionRepo::delete);
        quizRepo.deleteById(id);
        return "redirect:/admin/assessments?tab=quizzes";
    }

    // ========== LABS ==========
    @GetMapping("/labs")
    public String labs(@RequestParam(required = false) String branch, HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        List<Lab> labs = labRepo.findAll();
        if (branch != null && !branch.isBlank()) {
            labs = labs.stream().filter(l -> branch.equalsIgnoreCase(l.getBranch())).collect(Collectors.toList());
        }
        model.addAttribute("admin", admin);
        model.addAttribute("labs", labs);
        model.addAttribute("branch", branch);
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("activePage", "labs");
        return "admin/labs";
    }

    @GetMapping("/labs/create")
    public String labCreateForm(HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        model.addAttribute("admin", admin);
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("activePage", "labs");
        return "admin/lab-create";
    }

    @PostMapping("/labs/create")
    public String labCreate(@RequestParam String title,
                            @RequestParam String branch,
                            @RequestParam(required = false) String description,
                            @RequestParam(required = false) String dueDate,
                            @RequestParam(required = false) MultipartFile file,
                            HttpSession session) throws IOException {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        Lab lab = new Lab();
        lab.setTitle(title);
        lab.setBranch(branch);
        lab.setDescription(description);
        if (dueDate != null && !dueDate.isBlank()) lab.setDueDate(java.time.LocalDate.parse(dueDate));
        lab.setStatus("Published");
        lab.setCreatedAt(LocalDateTime.now());
        if (file != null && !file.isEmpty()) {
            lab.setFilePath(fileStorage.store(file, "labs"));
        }
        labRepo.save(lab);
        return "redirect:/admin/labs";
    }

    @GetMapping("/labs/submissions")
    public String labSubmissions(@RequestParam(required = false) String branch, HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        List<LabSubmission> subs = labSubmissionRepo.findAll();
        if (branch != null && !branch.isBlank()) {
            subs = subs.stream().filter(s -> branch.equalsIgnoreCase(s.getBranch())).collect(Collectors.toList());
        }
        model.addAttribute("admin", admin);
        model.addAttribute("submissions", subs);
        model.addAttribute("branch", branch);
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("activePage", "labs");
        return "admin/lab-submissions";
    }


    @PostMapping("/lab/{id}/launch")
    public String launchLabVm(@PathVariable Long id, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        LabVm vm = labVmRepo.findById(id).orElse(null);
        if (vm == null) return "redirect:/admin/lab";
        if (vm.getContainerId() != null && !vm.getContainerId().isBlank()) {
            return "redirect:/admin/lab?already=1";
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
            act.setUserId(requireAdmin(session) != null ? ((User)session.getAttribute("adminUser")).getId() : null);
            act.setUserName(requireAdmin(session) != null ? ((User)session.getAttribute("adminUser")).getName() : "admin");
            act.setUserRole("ADMIN");
            act.setAction("LAUNCH");
            act.setDetails("Launched " + vm.getOsType() + " port=" + result.port);
            labActivityRepo.save(act);
            return "redirect:/admin/lab/" + id + "/desktop";
        }
        return "redirect:/admin/lab?error=" + (result.error != null ? result.error.replace(" ", "_") : "docker_failed");
    }

    /** Embed noVNC desktop inside admin portal. */
    @GetMapping("/lab/{id}/desktop")
    public String adminLabDesktop(@PathVariable Long id, HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        LabVm vm = labVmRepo.findById(id).orElse(null);
        if (vm == null) return "redirect:/admin/lab";
        model.addAttribute("admin", admin);
        model.addAttribute("vm", vm);
        model.addAttribute("novncUrl", vm.getNovncUrl());
        model.addAttribute("activePage", "lab");
        return "admin/lab-desktop";
    }

    @PostMapping("/lab/{id}/stop")
    public String stopLabVm(@PathVariable Long id, HttpSession session) {
        if (requireAdmin(session) == null) return "redirect:/admin/login";
        LabVm vm = labVmRepo.findById(id).orElse(null);
        if (vm == null) return "redirect:/admin/lab";
        if (vm.getContainerId() != null) {
            dockerLabService.stop(vm.getContainerId());
        }
        vm.setContainerId(null);
        vm.setHostPort(0);
        vm.setNovncUrl(null);
        vm.setStatus(vm.getAssignedStudentId() != null ? "Assigned" : "Available");
        labVmRepo.save(vm);
        return "redirect:/admin/lab?stopped=1";
    }

    @GetMapping("/lab/activities")
    public String labActivities(HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        model.addAttribute("admin", admin);
        model.addAttribute("activities", labActivityRepo.findAllByOrderByCreatedAtDesc());
        model.addAttribute("activePage", "lab");
        return "admin/lab-activities";
    }

    @GetMapping("/lab/activities/export.pdf")
    public void exportLabActivitiesPdf(HttpSession session, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        if (requireAdmin(session) == null) { response.sendRedirect("/admin/login"); return; }
        var list = labActivityRepo.findAllByOrderByCreatedAtDesc();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=lab-activities.pdf");
        org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument();
        org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
        doc.addPage(page);
        var font = new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
        var fontBold = new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
        org.apache.pdfbox.pdmodel.PDPageContentStream cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);
        cs.beginText();
        cs.setFont(fontBold, 14);
        cs.newLineAtOffset(40, 750);
        cs.showText("Lab VM Activities");
        cs.setFont(font, 10);
        for (LabActivity a : list) {
            cs.newLineAtOffset(0, -14);
            String line = (a.getCreatedAt() != null ? a.getCreatedAt().toString() : "") + " | " +
                    (a.getUserName() != null ? a.getUserName() : "") + " | " + a.getAction() + " | VM#" + a.getLabVmId();
            if (line.length() > 90) line = line.substring(0, 90);
            cs.showText(line.replace("\n", " "));
        }
        cs.endText();
        cs.close();
        doc.save(response.getOutputStream());
        doc.close();
    }

    @GetMapping("/lab/activities/export.docx")
    public void exportLabActivitiesDocx(HttpSession session, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        if (requireAdmin(session) == null) { response.sendRedirect("/admin/login"); return; }
        var list = labActivityRepo.findAllByOrderByCreatedAtDesc();
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=lab-activities.docx");
        org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument();
        org.apache.poi.xwpf.usermodel.XWPFParagraph title = doc.createParagraph();
        title.createRun().setText("Lab VM Activities");
        for (LabActivity a : list) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph p = doc.createParagraph();
            p.createRun().setText(
                (a.getCreatedAt() != null ? a.getCreatedAt().toString() : "") + " | " +
                (a.getUserName() != null ? a.getUserName() : "") + " | " + a.getAction() +
                " | VM#" + a.getLabVmId() + " | " + (a.getDetails() != null ? a.getDetails() : "")
            );
        }
        doc.write(response.getOutputStream());
        doc.close();
    }
}
