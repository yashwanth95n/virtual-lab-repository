package com.ledger.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_attempts")
public class ExamAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long examId;
    private String examTitle;
    private Long studentId;
    private String studentName;
    private String branch;
    private int score;
    private int total;
    private double percent;
    private String letterGrade;
    private int tabSwitches;
    private boolean autoSubmitted;
    private String cameraSnapshotPath;
    private String videoPath;
    private String status; // InProgress, Submitted, Blocked, Blocked-Camera, Abandoned
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private String autoReason; // FULLSCREEN_EXIT, TAB_SWITCH, CAMERA_OFF, CAMERA_COVERED, SCREEN_SHARE_STOPPED, BROWSER_CLOSED
    private boolean retakeAllowed; // admin-granted (or auto-granted for a closed browser) retake, consumed on next start

    public ExamAttempt() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public String getExamTitle() { return examTitle; }
    public void setExamTitle(String examTitle) { this.examTitle = examTitle; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public double getPercent() { return percent; }
    public void setPercent(double percent) { this.percent = percent; }
    public String getLetterGrade() { return letterGrade; }
    public void setLetterGrade(String letterGrade) { this.letterGrade = letterGrade; }
    public int getTabSwitches() { return tabSwitches; }
    public void setTabSwitches(int tabSwitches) { this.tabSwitches = tabSwitches; }
    public boolean isAutoSubmitted() { return autoSubmitted; }
    public void setAutoSubmitted(boolean autoSubmitted) { this.autoSubmitted = autoSubmitted; }
    public String getCameraSnapshotPath() { return cameraSnapshotPath; }
    public void setCameraSnapshotPath(String cameraSnapshotPath) { this.cameraSnapshotPath = cameraSnapshotPath; }
    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getAutoReason() { return autoReason; }
    public void setAutoReason(String autoReason) { this.autoReason = autoReason; }
    public boolean isRetakeAllowed() { return retakeAllowed; }
    public void setRetakeAllowed(boolean retakeAllowed) { this.retakeAllowed = retakeAllowed; }
}
