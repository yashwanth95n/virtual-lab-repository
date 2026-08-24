package com.ledger.ssh;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.ledger.model.LabVm;
import com.ledger.repository.LabVmRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SshTerminalHandler extends TextWebSocketHandler {

    private final LabVmRepository labVmRepo;
    private final Map<String, SshBridge> bridges = new ConcurrentHashMap<>();

    public SshTerminalHandler(LabVmRepository labVmRepo) {
        this.labVmRepo = labVmRepo;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long vmId = extractVmId(session);
        LabVm vm = labVmRepo.findById(vmId).orElse(null);
        if (vm == null || vm.getSshHost() == null || vm.getSshHost().isBlank()) {
            session.sendMessage(new TextMessage("\r\n\u001b[31mNo SSH host configured for this lab VM.\u001b[0m\r\n"));
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        try {
            SshBridge bridge = connect(vm, session);
            bridges.put(session.getId(), bridge);
            session.sendMessage(new TextMessage("\r\n\u001b[32mConnected to Kali: " + vm.getSshHost() + "\u001b[0m\r\n"));
        } catch (Exception e) {
            session.sendMessage(new TextMessage("\r\n\u001b[31mSSH failed: " + e.getMessage() + "\u001b[0m\r\n"));
            session.sendMessage(new TextMessage("Check: VM running, SSH enabled, host/port/user/password.\r\n"));
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private SshBridge connect(LabVm vm, WebSocketSession ws) throws Exception {
        JSch jsch = new JSch();
        Session ssh = jsch.getSession(
                vm.getSshUsername() != null ? vm.getSshUsername() : "student",
                vm.getSshHost(),
                vm.getSshPort() > 0 ? vm.getSshPort() : 22
        );
        ssh.setPassword(vm.getSshPassword() != null ? vm.getSshPassword() : "");
        Properties cfg = new Properties();
        cfg.put("StrictHostKeyChecking", "no");
        ssh.setConfig(cfg);
        ssh.connect(15000);

        ChannelShell channel = (ChannelShell) ssh.openChannel("shell");
        channel.setPty(true);
        channel.setPtyType("xterm");
        channel.setPtySize(120, 30, 640, 480);
        InputStream in = channel.getInputStream();
        OutputStream out = channel.getOutputStream();
        channel.connect(5000);

        Thread reader = new Thread(() -> {
            byte[] buf = new byte[1024];
            try {
                int n;
                while ((n = in.read(buf)) != -1 && ws.isOpen()) {
                    String chunk = new String(buf, 0, n, StandardCharsets.UTF_8);
                    synchronized (ws) {
                        ws.sendMessage(new TextMessage(chunk));
                    }
                }
            } catch (Exception ignored) {
            }
        }, "ssh-reader-" + ws.getId());
        reader.setDaemon(true);
        reader.start();

        return new SshBridge(ssh, channel, out, reader);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SshBridge bridge = bridges.get(session.getId());
        if (bridge != null && bridge.out != null) {
            bridge.out.write(message.getPayload().getBytes(StandardCharsets.UTF_8));
            bridge.out.flush();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SshBridge bridge = bridges.remove(session.getId());
        if (bridge != null) bridge.close();
    }

    private Long extractVmId(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String[] parts = path.split("/");
        return Long.parseLong(parts[parts.length - 1]);
    }

    private static class SshBridge {
        final Session session;
        final ChannelShell channel;
        final OutputStream out;
        final Thread reader;

        SshBridge(Session session, ChannelShell channel, OutputStream out, Thread reader) {
            this.session = session;
            this.channel = channel;
            this.out = out;
            this.reader = reader;
        }

        void close() {
            try { channel.disconnect(); } catch (Exception ignored) {}
            try { session.disconnect(); } catch (Exception ignored) {}
        }
    }
}
