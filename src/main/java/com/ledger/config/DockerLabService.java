package com.ledger.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ubuntu GUI  → lscr.io/linuxserver/webtop:ubuntu-xfce  (HTTP :3000)
 * Kali GUI    → kasmweb/kali-rolling-desktop:1.18.0   (HTTPS :6901)
 */
@Service
public class DockerLabService {
/* we used app-linux:latest because we installed packages so students need not to install required commands everytime*/
    public static final String IMAGE_UBUNTU = "app-linux:latest";
    /** Real Kali desktop — public on Docker Hub (500K+ pulls). */
    public static final String IMAGE_KALI = "kasmweb/kali-rolling-desktop:1.18.0";

    @Value("${lab.docker.enabled:true}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public static class LaunchResult {
        public boolean success;
        public String containerId;
        public int port;
        public String novncUrl;
        public String error;
        public String imageType;
        public String loginHint;
    }

    public LaunchResult launchUbuntu() {
        return launchUbuntuInternal();
    }

    public LaunchResult launchKali() {
        return launchKaliInternal();
    }

    private LaunchResult launchUbuntuInternal() {
        LaunchResult r = new LaunchResult();
        r.imageType = "ubuntu";
        if (!enabled) {
            r.success = false;
            r.error = "Docker labs disabled";
            return r;
        }
        try {
            int hostPort = freePort();
            String name = "ledger-ubuntu-" + hostPort;
            tryRun("docker", "rm", "-f", name);

            List<String> cmd = new ArrayList<>();
            cmd.add("docker");
            cmd.add("run");
            cmd.add("-d");
            cmd.add("--rm");
            cmd.add("--shm-size=1g");
            cmd.add("-p");
            cmd.add(hostPort + ":3000");
            cmd.add("-e");
            cmd.add("PUID=1000");
            cmd.add("-e");
            cmd.add("PGID=1000");
            cmd.add("-e");
            cmd.add("TZ=Etc/UTC");
            cmd.add("--name");
            cmd.add(name);
            cmd.add(IMAGE_UBUNTU);

            String out = exec(cmd, 120);
            if (out == null) {
                pull(IMAGE_UBUNTU);
                out = exec(cmd, 120);
            }
            if (out == null || out.startsWith("ERR:")) {
                r.success = false;
                r.error = out != null ? out.substring(4) : "docker run failed";
                return r;
            }

            String id = out.trim().replace("\r", "").replace("\n", "");
            if (id.length() > 12) id = id.substring(0, 12);

            String url = "http://localhost:" + hostPort;
            waitHttp(url, 35);

            r.success = true;
            r.containerId = id;
            r.port = hostPort;
            r.novncUrl = url;
            r.loginHint = "Ubuntu desktop — no password required";
            return r;
        } catch (Exception e) {
            r.success = false;
            r.error = e.getMessage() != null ? e.getMessage() : "unknown";
            return r;
        }
    }

    private LaunchResult launchKaliInternal() {
        LaunchResult r = new LaunchResult();
        r.imageType = "kali";
        if (!enabled) {
            r.success = false;
            r.error = "Docker labs disabled";
            return r;
        }
        try {
            int hostPort = freePort();
            String name = "ledger-kali-" + hostPort;
            tryRun("docker", "rm", "-f", name);

            // Kasm Kali: HTTPS on container port 6901
            List<String> cmd = new ArrayList<>();
            cmd.add("docker");
            cmd.add("run");
            cmd.add("-d");
            cmd.add("--rm");
            cmd.add("--shm-size=512m");
            cmd.add("-p");
            cmd.add(hostPort + ":6901");
            cmd.add("-e");
            cmd.add("VNC_PW=password");
            cmd.add("--name");
            cmd.add(name);
            cmd.add(IMAGE_KALI);

            String out = exec(cmd, 180);
            if (out == null) {
                pull(IMAGE_KALI);
                out = exec(cmd, 180);
            }
            if (out == null || out.startsWith("ERR:")) {
                r.success = false;
                r.error = out != null ? out.substring(4) : "docker run failed";
                return r;
            }

            String id = out.trim().replace("\r", "").replace("\n", "");
            if (id.length() > 12) id = id.substring(0, 12);

            // HTTPS — browser may ask to accept self-signed cert
            String url = "https://localhost:" + hostPort;
            // Give Kali more time to boot
            Thread.sleep(8000);

            r.success = true;
            r.containerId = id;
            r.port = hostPort;
            r.novncUrl = url;
            r.loginHint = "User: kasm_user  |  Password: password  (accept browser security warning)";
            return r;
        } catch (Exception e) {
            r.success = false;
            r.error = e.getMessage() != null ? e.getMessage() : "unknown";
            return r;
        }
    }

    public boolean stop(String containerId) {
        if (containerId == null || containerId.isBlank()) return false;
        try {
            run("docker", "stop", containerId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns container id on success, null if image missing, "ERR:..." on other failure. */
    private String exec(List<String> cmd, int timeoutSec) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = readAll(p);
        boolean finished = p.waitFor(timeoutSec, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            return "ERR:timeout";
        }
        if (p.exitValue() != 0) {
            String lower = out.toLowerCase();
            if (lower.contains("not found") || lower.contains("unable to find")
                    || lower.contains("pull access") || lower.contains("manifest unknown")) {
                return null; // signal pull + retry
            }
            return "ERR:" + truncate(out);
        }
        return out;
    }

    private void pull(String image) throws Exception {
        ProcessBuilder pull = new ProcessBuilder("docker", "pull", image);
        pull.redirectErrorStream(true);
        Process pp = pull.start();
        readAll(pp);
        pp.waitFor(900, TimeUnit.SECONDS);
    }

    private boolean waitHttp(String baseUrl, int maxSeconds) {
        long deadline = System.currentTimeMillis() + maxSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(baseUrl).openConnection();
                c.setConnectTimeout(1500);
                c.setReadTimeout(1500);
                c.setRequestMethod("GET");
                int code = c.getResponseCode();
                c.disconnect();
                if (code >= 200 && code < 500) return true;
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return false;
    }

    private int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        }
    }

    private void tryRun(String... cmd) {
        try {
            run(cmd);
        } catch (Exception ignored) {
        }
    }

    private void run(String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        readAll(p);
        p.waitFor(45, TimeUnit.SECONDS);
    }

    private String readAll(Process p) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private String truncate(String s) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > 220 ? s.substring(0, 220) : s;
    }
}
