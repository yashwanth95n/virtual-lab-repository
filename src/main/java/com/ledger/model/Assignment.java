package com.ledger.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Long courseId;
    private String courseTitle;
    private Long instructorId;
    private String instructorName;
    private LocalDate dueDate;
    private int submissions;
    private int totalStudents;
    private String status;
    private String branch;
    private boolean dailyTask;
    private int dailyCount;   // questions per day
    @Lob
    @Column(columnDefinition = "TEXT")
    private String taskInstructions;
    private String sourceFilePath;  // CS, DITISS, etc          // Published, Draft
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;
    private String filePath;        // if uploaded questions
    private LocalDateTime createdAt;

    public Assignment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public Long getInstructorId() { return instructorId; }
    public void setInstructorId(Long instructorId) { this.instructorId = instructorId; }
    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public int getSubmissions() { return submissions; }
    public void setSubmissions(int submissions) { this.submissions = submissions; }
    public int getTotalStudents() { return totalStudents; }
    public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public boolean isDailyTask() { return dailyTask; }
    public void setDailyTask(boolean dailyTask) { this.dailyTask = dailyTask; }
    public int getDailyCount() { return dailyCount; }
    public void setDailyCount(int dailyCount) { this.dailyCount = dailyCount; }
    public String getTaskInstructions() { return taskInstructions; }
    public void setTaskInstructions(String taskInstructions) { this.taskInstructions = taskInstructions; }
    public String getSourceFilePath() { return sourceFilePath; }
    public void setSourceFilePath(String sourceFilePath) { this.sourceFilePath = sourceFilePath; }
}
