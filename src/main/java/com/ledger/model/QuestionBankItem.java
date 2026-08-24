package com.ledger.model;

import jakarta.persistence.*;

@Entity
@Table(name = "question_bank")
public class QuestionBankItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Lob @Column(columnDefinition = "TEXT")
    private String questionText;
    @Column(length = 1000) private String optionA;
    @Column(length = 1000) private String optionB;
    @Column(length = 1000) private String optionC;
    @Column(length = 1000) private String optionD;
    private String correctOption;
    private String courseName;
    private String topic;
    private String branch;
    private String difficulty; // Easy, Medium, Hard
    private int marks = 1;
    private Long instructorId;
    private String instructorName;

    public QuestionBankItem() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public String getCorrectOption() { return correctOption; }
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public int getMarks() { return marks; }
    public void setMarks(int marks) { this.marks = marks; }
    public Long getInstructorId() { return instructorId; }
    public void setInstructorId(Long instructorId) { this.instructorId = instructorId; }
    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
}
