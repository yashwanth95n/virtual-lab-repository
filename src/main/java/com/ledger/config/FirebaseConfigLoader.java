package com.ledger.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Loads Firebase Web config from classpath JSON (not application.properties).
 * File: src/main/resources/firebase-web-config.json
 */
@Component
public class FirebaseConfigLoader {

    private String apiKey = "";
    private String authDomain = "";
    private String projectId = "";
    private String appId = "";
    private boolean loaded;

    @PostConstruct
    public void load() {
        try {
            ClassPathResource res = new ClassPathResource("firebase-web-config.json");
            if (!res.exists()) {
                System.err.println("[Firebase] firebase-web-config.json not found on classpath");
                return;
            }
            try (InputStream in = res.getInputStream()) {
                JsonNode n = new ObjectMapper().readTree(in);
                apiKey = text(n, "apiKey");
                authDomain = text(n, "authDomain");
                projectId = text(n, "projectId");
                if (projectId.isBlank()) projectId = text(n, "project_id");
                appId = text(n, "appId");
                loaded = !apiKey.isBlank() && !apiKey.startsWith("PASTE_") && !apiKey.startsWith("YOUR_");
                System.out.println("[Firebase] Web config loaded from firebase-web-config.json (ready=" + loaded + ")");
            }
        } catch (Exception e) {
            System.err.println("[Firebase] Failed to load firebase-web-config.json: " + e.getMessage());
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? "" : v.asText("").trim();
    }

    public String getApiKey() { return apiKey; }
    public String getAuthDomain() { return authDomain; }
    public String getProjectId() { return projectId; }
    public String getAppId() { return appId; }
    public boolean isReady() { return loaded; }
}
