package com.ledger.config;

import com.ledger.model.Branch;
import com.ledger.model.PlatformSettings;
import com.ledger.model.User;
import com.ledger.repository.BranchRepository;
import com.ledger.repository.PlatformSettingsRepository;
import com.ledger.repository.UserRepository;
import com.ledger.util.PasswordUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    private final PlatformSettingsRepository settingsRepo;
    private final BranchRepository branchRepo;
    private final UserRepository userRepo;

    public DataLoader(PlatformSettingsRepository settingsRepo, BranchRepository branchRepo, UserRepository userRepo) {
        this.settingsRepo = settingsRepo;
        this.branchRepo = branchRepo;
        this.userRepo = userRepo;
    }

    @Override
    public void run(String... args) {
        if (settingsRepo.count() == 0) {
            settingsRepo.save(new PlatformSettings());
        }
        if (branchRepo.count() == 0) {
            branchRepo.save(new Branch("CS", "Computer Science"));
            branchRepo.save(new Branch("DITISS", "Diploma in IT Infrastructure Systems & Security"));
            branchRepo.save(new Branch("BDA", "Big Data Analytics"));
            branchRepo.save(new Branch("EC", "Electronics & Communication"));
            branchRepo.save(new Branch("Design", "Design"));
            branchRepo.save(new Branch("Business", "Business"));
        }
        // Default admin only if no admin exists (password: admin123 — change after login)
        boolean hasAdmin = userRepo.findByRole("ADMIN").stream().findAny().isPresent();
        if (!hasAdmin) {
            User admin = new User("System Admin", "admin@ledger.lms", "ADMIN", "Admin", "Active");
            admin.setPassword(PasswordUtil.hash("admin123"));
            admin.setEmailVerified(true);
            admin.setMobile("9999999999");
            admin.setJoinedAt(LocalDateTime.now());
            admin.setLastActive(LocalDateTime.now());
            userRepo.save(admin);
            System.out.println("[LedgerLMS] Default admin created: admin@ledger.lms / admin123 (change password after login)");
        }
    }
}
