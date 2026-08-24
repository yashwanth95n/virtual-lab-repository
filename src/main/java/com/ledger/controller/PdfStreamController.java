package com.ledger.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

/**
 * Streams PDF material bytes through an authenticated endpoint instead of
 * serving them from a static/public path. This is the piece that actually
 * matters for access control -- the front-end toolbar hiding is UX polish
 * on top of this.
 *
 * Replace the enrollment check / lookups with your real services.
 */
@RestController
@RequestMapping("/student/course")
public class PdfStreamController {

    // Inject your real services here.
    // private final CourseService courseService;
    // private final MaterialService materialService;
    // private final EnrollmentService enrollmentService;

    @GetMapping("/{courseId}/material/{materialId}/stream")
    public ResponseEntity<Resource> streamPdf(
            @PathVariable Long courseId,
            @PathVariable Long materialId,
            @AuthenticationPrincipal Object currentUser /* swap for your UserDetails type */) {

        // 1. Verify the current user is enrolled in courseId (403 if not).
        // if (!enrollmentService.isEnrolled(currentUser, courseId)) {
        //     return ResponseEntity.status(403).build();
        // }

        // 2. Look up the material and confirm it belongs to courseId and is type PDF.
        // Material material = materialService.getForCourse(courseId, materialId);
        // if (material == null || material.getType() != MaterialType.PDF) {
        //     return ResponseEntity.notFound().build();
        // }

        // 3. Resolve the file path OUTSIDE any publicly-served static directory,
        //    e.g. an app-data folder that has no direct URL mapping.
        Path filePath = Path.of("/var/lms-data/materials/" + materialId + ".pdf");
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                // "inline" renders in the iframe rather than triggering a download prompt
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"material.pdf\"")
                // Prevent the browser/proxy from caching a local copy
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                // Prevent this response from being framed by any other origin
                .header("X-Frame-Options", "SAMEORIGIN")
                .body(resource);
    }
}
