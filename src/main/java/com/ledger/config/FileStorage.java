package com.ledger.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Component
public class FileStorage {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public String store(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) return null;
        Path dir = Paths.get(uploadDir, folder).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String original = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String filename = System.currentTimeMillis() + "_" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
        Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + folder + "/" + filename;
    }

    public Path resolve(String relativeWebPath) {
        // /uploads/pdfs/xxx.pdf -> uploads/pdfs/xxx.pdf
        String rel = relativeWebPath.startsWith("/uploads/")
                ? relativeWebPath.substring("/uploads/".length())
                : relativeWebPath;
        return Paths.get(uploadDir, rel).toAbsolutePath().normalize();
    }
}
