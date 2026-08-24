package com.ledger.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String category;          // Computer Science, Design, Business, etc.
    private String instructorName;
    private Long instructorId;
    private int enrolledCount;
    private String status;            // Published, Draft, Archived
    private boolean published;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;
    private String coverImage;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseMaterial> materials = new ArrayList<>();

    public Course() {}

    public Course(String title, String category, String instructorName, String status) {
        this.title = title;
        this.category = category;
        this.instructorName = instructorName;
        this.status = status;
        this.published = "Published".equals(status);
        this.enrolledCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
    public Long getInstructorId() { return instructorId; }
    public void setInstructorId(Long instructorId) { this.instructorId = instructorId; }
    public int getEnrolledCount() { return enrolledCount; }
    public void setEnrolledCount(int enrolledCount) { this.enrolledCount = enrolledCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; this.published = "Published".equals(status); }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<CourseMaterial> getMaterials() { return materials; }
    public void setMaterials(List<CourseMaterial> materials) { this.materials = materials; }
}
