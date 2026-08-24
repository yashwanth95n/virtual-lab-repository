package com.ledger.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_vms")
public class LabVm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;              // e.g. Kali Linux Lab 1
    private String osType;            // Kali Linux
    private String accessUrl;         // simulated /lab/session/{id}
    private String status;            // Available, Assigned, Running
    private Long assignedStudentId;
    private String assignedStudentName;
    private Long createdByAdminId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime lastSavedAt;
    private String savedFilePath;     // student saved work path for admin
    private String sshHost;           // e.g. 192.168.1.50 or localhost
    private int sshPort = 22;
    private String sshUsername;       // student account on Kali
    private String sshPassword;
    private String branch;            // assigned branch
    private String containerId;       // Docker container id
    private int hostPort;             // host port mapped to noVNC (3000)
    private String novncUrl;          // http://localhost:{port}

    public LabVm() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOsType() { return osType; }
    public void setOsType(String osType) { this.osType = osType; }
    public String getAccessUrl() { return accessUrl; }
    public void setAccessUrl(String accessUrl) { this.accessUrl = accessUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAssignedStudentId() { return assignedStudentId; }
    public void setAssignedStudentId(Long assignedStudentId) { this.assignedStudentId = assignedStudentId; }
    public String getAssignedStudentName() { return assignedStudentName; }
    public void setAssignedStudentName(String assignedStudentName) { this.assignedStudentName = assignedStudentName; }
    public Long getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(Long createdByAdminId) { this.createdByAdminId = createdByAdminId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastSavedAt() { return lastSavedAt; }
    public void setLastSavedAt(LocalDateTime lastSavedAt) { this.lastSavedAt = lastSavedAt; }
    public String getSavedFilePath() { return savedFilePath; }
    public void setSavedFilePath(String savedFilePath) { this.savedFilePath = savedFilePath; }
    public String getSshHost() { return sshHost; }
    public void setSshHost(String sshHost) { this.sshHost = sshHost; }
    public int getSshPort() { return sshPort; }
    public void setSshPort(int sshPort) { this.sshPort = sshPort; }
    public String getSshUsername() { return sshUsername; }
    public void setSshUsername(String sshUsername) { this.sshUsername = sshUsername; }
    public String getSshPassword() { return sshPassword; }
    public void setSshPassword(String sshPassword) { this.sshPassword = sshPassword; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }
    public int getHostPort() { return hostPort; }
    public void setHostPort(int hostPort) { this.hostPort = hostPort; }
    public String getNovncUrl() { return novncUrl; }
    public void setNovncUrl(String novncUrl) { this.novncUrl = novncUrl; }
}
