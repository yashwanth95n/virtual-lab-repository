package com.ledger.controller;

import com.ledger.model.PlatformSettings;
import com.ledger.model.User;
import com.ledger.repository.BranchRepository;
import com.ledger.repository.PlatformSettingsRepository;
import com.ledger.repository.UserRepository;
import com.ledger.util.PasswordUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import com.ledger.config.MailService;
import com.ledger.config.FirebaseConfigLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
public class AuthController {

    private final UserRepository userRepo;
    private final BranchRepository branchRepo;
    private final PlatformSettingsRepository settingsRepo;
    private final MailService mailService;
    private final FirebaseConfigLoader firebaseConfig;

    public AuthController(UserRepository userRepo, BranchRepository branchRepo,
                          PlatformSettingsRepository settingsRepo,
                          MailService mailService,
                          FirebaseConfigLoader firebaseConfig) {
        this.userRepo = userRepo;
        this.branchRepo = branchRepo;
        this.settingsRepo = settingsRepo;
        this.mailService = mailService;
        this.firebaseConfig = firebaseConfig;
    }

    private PlatformSettings settings() {
        return settingsRepo.findAll().stream().findFirst().orElseGet(() -> {
            PlatformSettings s = new PlatformSettings();
            return settingsRepo.save(s);
        });
    }

    private void addFirebase(Model model) {
        model.addAttribute("firebaseApiKey", firebaseConfig.getApiKey());
        model.addAttribute("firebaseAuthDomain", firebaseConfig.getAuthDomain());
        model.addAttribute("firebaseProjectId", firebaseConfig.getProjectId());
        model.addAttribute("firebaseAppId", firebaseConfig.getAppId());
        model.addAttribute("firebaseReady", firebaseConfig.isReady());
    }


    private boolean validMobile(String mobile) {
        return mobile != null && mobile.matches("\\d{10}");
    }

    private boolean studentPortalOpen() {
        return settings().isStudentPortalEnabled();
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    // ========== UNIFIED LOGIN (students + admins) ==========
    @GetMapping("/login")
    public String loginPage(Model model) {
        addFirebase(model);
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session, Model model) {
        addFirebase(model);
        Optional<User> opt = userRepo.findByEmail(email);
        if (opt.isEmpty() || !("STUDENT".equals(opt.get().getRole()) || "ADMIN".equals(opt.get().getRole()))) {
            model.addAttribute("error", "Invalid email or password");
            return "auth/login";
        }
        User u = opt.get();
        boolean isStudent = "STUDENT".equals(u.getRole());

        if (isStudent && !studentPortalOpen()) {
            model.addAttribute("portalDisabled", true);
            return "auth/student-disabled";
        }
        if (u.getLockUntil() != null && u.getLockUntil().isAfter(LocalDateTime.now())) {
            model.addAttribute("error", "Account locked due to too many failed attempts. Try again after 5 minutes.");
            return "auth/login";
        }
        if (!PasswordUtil.matches(password, u.getPassword())) {
            u.setFailedLoginAttempts(u.getFailedLoginAttempts() + 1);
            if (u.getFailedLoginAttempts() >= 5) {
                u.setLockUntil(LocalDateTime.now().plusMinutes(5));
                u.setFailedLoginAttempts(0);
                userRepo.save(u);
                model.addAttribute("error", "5 failed attempts. Account locked for 5 minutes.");
                return "auth/login";
            }
            userRepo.save(u);
            model.addAttribute("error", "Invalid email or password (" + (5 - u.getFailedLoginAttempts()) + " attempts left)");
            return "auth/login";
        }
        u.setFailedLoginAttempts(0);
        u.setLockUntil(null);
        u.setLastActive(LocalDateTime.now());
        // Upgrade legacy plain password to BCrypt
        if (!u.getPassword().startsWith("$2")) {
            u.setPassword(PasswordUtil.hash(password));
        }
        userRepo.save(u);

        if (isStudent) {
            session.setAttribute("studentUser", u);
            return "redirect:/student/dashboard";
        } else {
            session.setAttribute("adminUser", u);
            return "redirect:/admin/dashboard";
        }
    }

    // ========== STUDENT REGISTER ==========
    @GetMapping("/student/register")
    public String studentRegisterPage(Model model) {
        if (!studentPortalOpen()) {
            model.addAttribute("portalDisabled", true);
            return "auth/student-disabled";
        }
        model.addAttribute("branches", branchRepo.findAll());
        return "auth/student-register";
    }

    @PostMapping("/student/register")
    public String studentRegister(@RequestParam String name,
                                  @RequestParam String email,
                                  @RequestParam String password,
                                  @RequestParam(required = false) String rollNo,
                                  @RequestParam(required = false) String gender,
                                  @RequestParam(required = false) String mobile,
                                  @RequestParam(required = false) String branch,
                                  Model model) {
        if (!studentPortalOpen()) {
            model.addAttribute("portalDisabled", true);
            return "auth/student-disabled";
        }
        model.addAttribute("branches", branchRepo.findAll());

        if (password == null || password.length() < 6 || password.length() > 16) {
            model.addAttribute("error", "Password must be 6–16 characters");
            return "auth/student-register";
        }
        if (!validMobile(mobile)) {
            model.addAttribute("error", "Mobile number must be exactly 10 digits");
            return "auth/student-register";
        }
        if (rollNo == null || rollNo.isBlank()) {
            model.addAttribute("error", "Roll number is required");
            return "auth/student-register";
        }
        if (userRepo.findByEmail(email).isPresent()) {
            model.addAttribute("error", "Email already registered");
            return "auth/student-register";
        }
        User u = new User(name, email, "STUDENT", branch, "Active");
        u.setPassword(PasswordUtil.hash(password));
        u.setRollNo(rollNo);
        u.setGender(gender);
        u.setMobile(mobile);
        u.setBranch(branch);
        u.setEmailVerified(true);
        u.setOtpCode(null);
        u.setOtpExpiry(null);
        userRepo.save(u);
        model.addAttribute("success", "Registration successful! Please sign in.");
        addFirebase(model);
        return "auth/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ========== PASSWORD RESET (token stored in DB) — works for students and admins ==========
    @GetMapping("/reset-password")
    public String resetPage() { return "auth/reset-password"; }

    @PostMapping("/reset-password")
    public String resetRequest(@RequestParam String email,
                               @RequestParam String password,
                               Model model) {
        Optional<User> opt = userRepo.findByEmail(email);
        if (opt.isEmpty() || !("STUDENT".equals(opt.get().getRole()) || "ADMIN".equals(opt.get().getRole()))) {
            model.addAttribute("error", "No account with that email");
            return "auth/reset-password";
        }
        if (password == null || password.length() < 6 || password.length() > 16) {
            model.addAttribute("error", "Password must be 6–16 characters");
            return "auth/reset-password";
        }
        User u = opt.get();
        u.setPassword(PasswordUtil.hash(password));
        u.setResetToken(null);
        u.setResetTokenExpiry(null);
        u.setFailedLoginAttempts(0);
        u.setLockUntil(null);
        userRepo.save(u);
        model.addAttribute("success", "Password updated. Please sign in.");
        addFirebase(model);
        return "auth/login";
    }

    // Backward-compatible aliases so any old bookmarked links still work.
    @GetMapping("/student/reset-password")
    public String studentResetAlias() { return "redirect:/reset-password"; }

    @GetMapping("/admin/reset-password")
    public String adminResetAlias() { return "redirect:/reset-password"; }

    

    /** Google sign-in callback: Firebase verifies identity client-side; we create/login user by email.
     *  Role is taken from the existing account, not from the client — new sign-ins become students. */
    @PostMapping("/auth/google")
    public String googleAuth(@RequestParam String email,
                             @RequestParam(required = false) String name,
                             HttpSession session, Model model) {
        Optional<User> opt = userRepo.findByEmail(email);
        User u;
        if (opt.isPresent()) {
            u = opt.get();
        } else {
            if (!studentPortalOpen()) {
                model.addAttribute("portalDisabled", true);
                return "auth/student-disabled";
            }
            u = new User(name != null ? name : email, email, "STUDENT", "", "Active");
            u.setPassword(PasswordUtil.hash(UUID.randomUUID().toString()));
            u.setEmailVerified(true);
            userRepo.save(u);
        }
        if ("ADMIN".equals(u.getRole())) {
            session.setAttribute("adminUser", u);
            return "redirect:/admin/dashboard";
        }
        if ("STUDENT".equals(u.getRole())) {
            if (!studentPortalOpen()) {
                model.addAttribute("portalDisabled", true);
                return "auth/student-disabled";
            }
            session.setAttribute("studentUser", u);
            return "redirect:/student/dashboard";
        }
        model.addAttribute("error", "This account has no recognized role");
        addFirebase(model);
        return "auth/login";
    }
}
