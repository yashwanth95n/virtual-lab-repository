package com.ledger.controller;

import com.ledger.config.FileStorage;
import com.ledger.config.GradeUtil;
import com.ledger.config.QuizTextParser;
import com.ledger.model.*;
import com.ledger.repository.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ExamController {

    private final ExamRepository examRepo;
    private final ExamQuestionRepository examQRepo;
    private final ExamAttemptRepository attemptRepo;
    private final BranchRepository branchRepo;
    private final QuestionBankRepository bankRepo;
    private final UserRepository userRepo;
    private final LabSubmissionRepository labSubmissionRepo;
    private final QuizAttemptRepository quizAttemptRepo;
    private final FileStorage fileStorage;

    public ExamController(ExamRepository examRepo, ExamQuestionRepository examQRepo,
                          ExamAttemptRepository attemptRepo, BranchRepository branchRepo,
                          QuestionBankRepository bankRepo, UserRepository userRepo,
                          LabSubmissionRepository labSubmissionRepo,
                          QuizAttemptRepository quizAttemptRepo, FileStorage fileStorage) {
        this.examRepo = examRepo;
        this.examQRepo = examQRepo;
        this.attemptRepo = attemptRepo;
        this.branchRepo = branchRepo;
        this.bankRepo = bankRepo;
        this.userRepo = userRepo;
        this.labSubmissionRepo = labSubmissionRepo;
        this.quizAttemptRepo = quizAttemptRepo;
        this.fileStorage = fileStorage;
    }

    private User admin(HttpSession s) {
        User u = (User) s.getAttribute("adminUser");
        return (u != null && "ADMIN".equals(u.getRole())) ? u : null;
    }
    private User student(HttpSession s) {
        User u = (User) s.getAttribute("studentUser");
        return (u != null && "STUDENT".equals(u.getRole())) ? u : null;
    }

    @GetMapping("/admin/exams")
    public String list(HttpSession session) {
        User a = admin(session);
        if (a == null) return "redirect:/admin/login";
        return "redirect:/admin/assessments?tab=exams";
    }

    @GetMapping("/admin/exams/create")
    public String createForm(HttpSession session, Model model) {
        User a = admin(session);
        if (a == null) return "redirect:/admin/login";
        model.addAttribute("admin", a);
        model.addAttribute("branches", branchRepo.findAll());
        model.addAttribute("bankCount", bankRepo.count());
        model.addAttribute("activePage", "exams");
        return "admin/exam-create";
    }

    @PostMapping("/admin/exams/create")
    public String create(@RequestParam String title,
                         @RequestParam(required = false) String subject,
                         @RequestParam String branch,
                         @RequestParam(defaultValue = "60") int durationMinutes,
                         @RequestParam(required = false) String instructions,
                         @RequestParam(required = false) MultipartFile coverFile,
                         @RequestParam(required = false) MultipartFile questionFile,
                         @RequestParam(required = false) String pasteContent,
                         @RequestParam(required = false, defaultValue = "0") int questionLimit,
                         @RequestParam(required = false, defaultValue = "false") boolean saveToBank,
                         @RequestParam(required = false) String topic,
                         HttpSession session, Model model) throws Exception {
        User a = admin(session);
        if (a == null) return "redirect:/admin/login";
        Exam exam = new Exam();
        exam.setTitle(title);
        exam.setSubject(subject != null ? subject.trim() : "");
        exam.setBranch(branch);
        exam.setDurationMinutes(durationMinutes);
        exam.setInstructions(instructions);
        exam.setStatus("Published");
        exam.setResultsPublished(false);
        exam.setMaxAttempts(1);
        exam.setCreatedAt(LocalDateTime.now());
        if (coverFile != null && !coverFile.isEmpty()) {
            exam.setCoverImage(fileStorage.store(coverFile, "covers"));
        }
        exam = examRepo.save(exam);

        String content = "";
        if (questionFile != null && !questionFile.isEmpty()) {
            content = QuizTextParser.extractText(questionFile);
        }
        if ((content == null || content.isBlank()) && pasteContent != null) content = pasteContent;

        List<QuizTextParser.ParsedQ> all = QuizTextParser.parse(content);
        if (all.isEmpty() && questionLimit > 0) {
            // draw from question bank
            List<QuestionBankItem> bank = bankRepo.findByBranch(branch);
            if (bank.isEmpty()) bank = bankRepo.findAll();
            Collections.shuffle(bank);
            int n = Math.min(questionLimit, bank.size());
            for (int i = 0; i < n; i++) {
                QuestionBankItem b = bank.get(i);
                ExamQuestion q = new ExamQuestion();
                q.setExamId(exam.getId());
                q.setQuestionText(b.getQuestionText());
                q.setOptionA(b.getOptionA()); q.setOptionB(b.getOptionB());
                q.setOptionC(b.getOptionC()); q.setOptionD(b.getOptionD());
                q.setCorrectOption(b.getCorrectOption());
                q.setOrderIndex(i);
                examQRepo.save(q);
            }
        } else {
            if (questionLimit > 0 && questionLimit < all.size()) {
                Collections.shuffle(all);
                all = new ArrayList<>(all.subList(0, questionLimit));
            }
            int idx = 0;
            for (QuizTextParser.ParsedQ pq : all) {
                ExamQuestion q = new ExamQuestion();
                q.setExamId(exam.getId());
                q.setQuestionText(pq.text);
                q.setOptionA(pq.a); q.setOptionB(pq.b); q.setOptionC(pq.c); q.setOptionD(pq.d);
                q.setCorrectOption(pq.ans);
                q.setOrderIndex(idx++);
                examQRepo.save(q);
                if (saveToBank) {
                    QuestionBankItem bi = new QuestionBankItem();
                    bi.setQuestionText(pq.text);
                    bi.setOptionA(pq.a); bi.setOptionB(pq.b); bi.setOptionC(pq.c); bi.setOptionD(pq.d);
                    bi.setCorrectOption(pq.ans);
                    bi.setBranch(branch);
                    bi.setTopic(topic != null ? topic : title);
                    bi.setInstructorId(a.getId());
                    bi.setInstructorName(a.getName());
                    bi.setMarks(1);
                    bi.setDifficulty("Medium");
                    bankRepo.save(bi);
                }
            }
        }
        return "redirect:/admin/exams/" + exam.getId();
    }

    @GetMapping("/admin/exams/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(required = false, defaultValue = "questions") String tab,
                         HttpSession session, Model model) {
        User a = admin(session);
        if (a == null) return "redirect:/admin/login";
        Exam exam = examRepo.findById(id).orElse(null);
        if (exam == null) return "redirect:/admin/exams";
        // Full list - used by the Videos and Activity tabs so abandoned/blocked
        // attempts with no score still show up (and can be granted a retake).
        List<ExamAttempt> attempts = attemptRepo.findByExamId(id).stream()
                .sorted(Comparator.comparing(ExamAttempt::getId).reversed())
                .collect(Collectors.toList());
        // Graded-only, ranked by percent - used by the Results tab.
        List<ExamAttempt> ranked = attempts.stream()
                .filter(at -> at.getTotal() > 0)
                .sorted(Comparator.comparingDouble(ExamAttempt::getPercent).reversed())
                .collect(Collectors.toList());
        Map<Long, String> rollMap = new HashMap<>();
        for (ExamAttempt at : attempts) {
            userRepo.findById(at.getStudentId()).ifPresent(u ->
                rollMap.put(at.getStudentId(), u.getRollNo() != null ? u.getRollNo() : "—"));
        }
        model.addAttribute("admin", a);
        model.addAttribute("exam", exam);
        model.addAttribute("questions", examQRepo.findByExamIdOrderByOrderIndexAsc(id));
        model.addAttribute("attempts", attempts);
        model.addAttribute("ranked", ranked);
        model.addAttribute("rollMap", rollMap);
        model.addAttribute("tab", tab);
        model.addAttribute("activePage", "exams");
        return "admin/exam-detail";
    }

    @PostMapping("/admin/exams/{id}/publish-results")
    public String publishResults(@PathVariable Long id, HttpSession session) {
        if (admin(session) == null) return "redirect:/admin/login";
        examRepo.findById(id).ifPresent(e -> {
            e.setResultsPublished(true);
            examRepo.save(e);
        });
        return "redirect:/admin/exams/" + id + "?tab=results";
    }

    @PostMapping("/admin/exams/{id}/reopen")
    public String reopen(@PathVariable Long id, @RequestParam Long studentId, HttpSession session) {
        if (admin(session) == null) return "redirect:/admin/login";
        // Grant a retake to this ONE student only - does not touch exam.maxAttempts,
        // so other students' attempt limits are unaffected.
        attemptRepo.findByExamId(id).stream()
                .filter(at -> studentId.equals(at.getStudentId()))
                .max(Comparator.comparing(ExamAttempt::getId))
                .ifPresent(at -> {
                    at.setRetakeAllowed(true);
                    attemptRepo.save(at);
                });
        return "redirect:/admin/exams/" + id + "?tab=results";
    }

    @PostMapping("/admin/exams/{id}/delete")
    public String deleteExam(@PathVariable Long id, HttpSession session) {
        if (admin(session) == null) return "redirect:/admin/login";
        // delete questions and attempts first
        examQRepo.findByExamIdOrderByOrderIndexAsc(id).forEach(examQRepo::delete);
        attemptRepo.findByExamId(id).forEach(attemptRepo::delete);
        examRepo.deleteById(id);
        return "redirect:/admin/exams";
    }

    @GetMapping("/admin/exams/{id}/export")
    public void exportExcel(@PathVariable Long id, HttpSession session, HttpServletResponse response) throws Exception {
        if (admin(session) == null) {
            response.sendRedirect("/admin/login");
            return;
        }
        Exam exam = examRepo.findById(id).orElse(null);
        if (exam == null) {
            response.sendRedirect("/admin/exams");
            return;
        }
        List<ExamAttempt> attempts = attemptRepo.findByExamId(id).stream()
                .sorted(Comparator.comparingDouble(ExamAttempt::getPercent).reversed())
                .collect(Collectors.toList());
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=exam-results-" + id + ".csv");
        PrintWriter w = response.getWriter();
        w.println("Rank,Name,Roll No,Branch,Score,Total,Percent,Grade,Tab Switches,Auto Submitted,Status");
        int rank = 1;
        for (ExamAttempt at : attempts) {
            User stu = userRepo.findById(at.getStudentId()).orElse(null);
            String roll = stu != null && stu.getRollNo() != null ? stu.getRollNo() : "";
            w.printf("%d,%s,%s,%s,%d,%d,%.1f,%s,%d,%s,%s%n",
                    rank++,
                    csv(at.getStudentName()),
                    csv(roll),
                    csv(at.getBranch()),
                    at.getScore(), at.getTotal(), at.getPercent(),
                    at.getLetterGrade() != null ? at.getLetterGrade() : "",
                    at.getTabSwitches(),
                    at.isAutoSubmitted() ? "Yes" : "No",
                    at.getStatus());
        }
        w.flush();
    }

    @GetMapping("/admin/results/export-all")
    public void exportAllResults(HttpSession session, HttpServletResponse response) throws Exception {
        if (admin(session) == null) {
            response.sendRedirect("/admin/login");
            return;
        }
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=all-student-results.csv");
        PrintWriter w = response.getWriter();
        w.println("Name,Roll No,Branch,Quiz Avg %,Exam Avg %,Lab Submissions,Overall Grade,Rank");
        List<User> students = userRepo.findByRole("STUDENT").stream()
                .filter(u -> !"Deleted".equals(u.getStatus()))
                .collect(Collectors.toList());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (User s : students) {
            // latest quiz only per quiz
            Map<Long, QuizAttempt> latestQ = new LinkedHashMap<>();
            for (QuizAttempt qa : quizAttemptRepo.findByStudentId(s.getId())) {
                QuizAttempt prev = latestQ.get(qa.getQuizId());
                if (prev == null || (qa.getAttemptedAt() != null && (prev.getAttemptedAt() == null || qa.getAttemptedAt().isAfter(prev.getAttemptedAt())))) {
                    latestQ.put(qa.getQuizId(), qa);
                }
            }
            double quizAvg = latestQ.values().stream().mapToDouble(QuizAttempt::getPercent).average().orElse(0);
            double examAvg = attemptRepo.findByStudentId(s.getId()).stream()
                    .filter(a -> a.getTotal() > 0)
                    .mapToDouble(ExamAttempt::getPercent).average().orElse(0);
            long labs = labSubmissionRepo.findByStudentId(s.getId()).size();
            double overall = (quizAvg + examAvg) / 2.0;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", s.getName());
            row.put("roll", s.getRollNo() != null ? s.getRollNo() : "");
            row.put("branch", s.getBranch() != null ? s.getBranch() : "");
            row.put("quiz", quizAvg);
            row.put("exam", examAvg);
            row.put("labs", labs);
            row.put("overall", overall);
            row.put("grade", overall > 0 ? GradeUtil.letter(overall) : "—");
            rows.add(row);
        }
        rows.sort((a, b) -> Double.compare((double) b.get("overall"), (double) a.get("overall")));
        int rank = 1;
        for (Map<String, Object> r : rows) {
            w.printf("%s,%s,%s,%.1f,%.1f,%d,%s,%d%n",
                    csv((String) r.get("name")), csv((String) r.get("roll")), csv((String) r.get("branch")),
                    (double) r.get("quiz"), (double) r.get("exam"), (long) r.get("labs"),
                    r.get("grade"), rank++);
        }
        w.flush();
    }

    private String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    @PostMapping("/admin/exams/{id}/answer-key")
    public String uploadAnswerKey(@PathVariable Long id,
                                  @RequestParam(required = false) MultipartFile answerFile,
                                  @RequestParam(required = false) String pasteAnswers,
                                  HttpSession session) throws Exception {
        if (admin(session) == null) return "redirect:/admin/login";
        String content = "";
        if (answerFile != null && !answerFile.isEmpty()) content = QuizTextParser.extractText(answerFile);
        if ((content == null || content.isBlank()) && pasteAnswers != null) content = pasteAnswers;
        List<ExamQuestion> qs = examQRepo.findByExamIdOrderByOrderIndexAsc(id);
        List<QuizTextParser.ParsedQ> parsed = QuizTextParser.parse(content);
        if (!parsed.isEmpty()) {
            for (int i = 0; i < Math.min(qs.size(), parsed.size()); i++) {
                qs.get(i).setCorrectOption(parsed.get(i).ans);
                examQRepo.save(qs.get(i));
            }
        }
        return "redirect:/admin/exams/" + id + "?tab=questions";
    }

    // ========== STUDENT ==========
    @GetMapping("/student/exams")
    public String studentExams(HttpSession session) {
        User s = student(session);
        if (s == null) return "redirect:/student/login";
        return "redirect:/student/assessments?tab=exams";
    }

    @GetMapping({"/student/exams/{id}/start", "/student/exams/{id}/live"})
    public String startExam(@PathVariable Long id, HttpSession session, Model model) {
        User s = student(session);
        if (s == null) return "redirect:/student/login";
        Exam exam = examRepo.findById(id).orElse(null);
        if (exam == null || !"Published".equalsIgnoreCase(exam.getStatus())) return "redirect:/student/exams";

        List<ExamAttempt> mine = attemptRepo.findByExamId(id).stream()
                .filter(at -> s.getId().equals(at.getStudentId()))
                .collect(Collectors.toList());

        // A student who closes the browser (or crashes) mid-exam never reaches
        // /submit, so the attempt is stuck "InProgress" forever with no video/score.
        // The in-page timer would already have auto-submitted well before the
        // exam's duration elapsed, so if we're still seeing "InProgress" long
        // after that, nothing ever made it to the server - treat it as an
        // abandoned browser-close and free up the attempt slot automatically,
        // no admin needed.
        for (ExamAttempt at : mine) {
            if ("InProgress".equals(at.getStatus()) && at.getStartedAt() != null) {
                LocalDateTime staleAfter = at.getStartedAt().plusMinutes(exam.getDurationMinutes() + 5);
                if (LocalDateTime.now().isAfter(staleAfter)) {
                    at.setStatus("Abandoned");
                    at.setAutoReason("BROWSER_CLOSED");
                    attemptRepo.save(at);
                }
            }
        }

        // Abandoned (browser-closed) attempts never count against the limit.
        // Attempts an admin has explicitly granted a retake for don't count either.
        long blockingCount = mine.stream()
                .filter(at -> !"Abandoned".equals(at.getStatus()))
                .filter(at -> !at.isRetakeAllowed())
                .count();

        if (blockingCount >= exam.getMaxAttempts()) {
            model.addAttribute("student", s);
            model.addAttribute("exam", exam);
            model.addAttribute("blocked", true);
            model.addAttribute("activePage", "exams");
            return "student/exam-blocked";
        }

        // Consume any retake grant now that it's being used, so it can't be reused.
        mine.stream().filter(ExamAttempt::isRetakeAllowed).forEach(at -> {
            at.setRetakeAllowed(false);
            attemptRepo.save(at);
        });

        ExamAttempt attempt = new ExamAttempt();
        attempt.setExamId(exam.getId());
        attempt.setExamTitle(exam.getTitle());
        attempt.setStudentId(s.getId());
        attempt.setStudentName(s.getName());
        attempt.setBranch(s.getBranch());
        attempt.setStatus("InProgress");
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setTabSwitches(0);
        attempt = attemptRepo.save(attempt);

        List<ExamQuestion> questions = new ArrayList<>(examQRepo.findByExamIdOrderByOrderIndexAsc(id));
        Collections.shuffle(questions);

        model.addAttribute("student", s);
        model.addAttribute("exam", exam);
        model.addAttribute("attempt", attempt);
        model.addAttribute("questions", questions);
        model.addAttribute("activePage", "exams");
        return "student/exam-live";
    }

    @PostMapping("/student/exams/{id}/submit")
    public String submitExam(@PathVariable Long id,
                             @RequestParam Long attemptId,
                             @RequestParam(defaultValue = "0") int tabSwitches,
                             @RequestParam(defaultValue = "false") boolean autoSubmitted,
                             @RequestParam(required = false) String autoReason,
                             @RequestParam(required = false) String cameraData,
                             @RequestParam(required = false) MultipartFile videoFile,
                             @RequestParam Map<String, String> allParams,
                             HttpSession session) throws Exception {
        User s = student(session);
        if (s == null) return "redirect:/student/login";
        Exam exam = examRepo.findById(id).orElse(null);
        ExamAttempt attempt = attemptRepo.findById(attemptId).orElse(null);
        if (exam == null || attempt == null) return "redirect:/student/exams";

        List<ExamQuestion> questions = examQRepo.findByExamIdOrderByOrderIndexAsc(id);
        int correct = 0;
        for (ExamQuestion q : questions) {
            String ans = allParams.get("q_" + q.getId());
            if (ans != null && ans.equalsIgnoreCase(q.getCorrectOption())) correct++;
        }
        int total = questions.size();
        attempt.setScore(correct);
        attempt.setTotal(total);
        // Grade only when total is exactly 20 marks
        if (GradeUtil.isGradable(total)) {
            attempt.setPercent(GradeUtil.percentFromMarks(correct, total));
            attempt.setLetterGrade(GradeUtil.letterFromMarks(correct, total));
        } else {
            attempt.setPercent(-1);
            attempt.setLetterGrade("—");
        }
        attempt.setTabSwitches(tabSwitches);
        attempt.setAutoSubmitted(autoSubmitted);
        attempt.setAutoReason(autoReason);
        if (autoSubmitted && autoReason != null && autoReason.toLowerCase().contains("camera")) {
            attempt.setStatus("Blocked-Camera");
        } else {
            attempt.setStatus(autoSubmitted ? "Blocked" : "Submitted");
        }
        attempt.setSubmittedAt(LocalDateTime.now());
        if (cameraData != null && cameraData.startsWith("data:image")) {
            attempt.setCameraSnapshotPath("captured");
        }
        if (videoFile != null && !videoFile.isEmpty()) {
            attempt.setVideoPath(fileStorage.store(videoFile, "exam-videos"));
        }
        attemptRepo.save(attempt);
        return "redirect:/student/exams?done=1";
    }
}
