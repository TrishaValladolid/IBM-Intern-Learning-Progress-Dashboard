package com.dashboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "assignment")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @Column(name = "max_score")
    private Integer maxScore;

    private String batch;

    // Which training this assignment belongs to, e.g. "Java Training",
    // "Japanese Training". Free text so it lines up with the training names
    // used elsewhere (batch training assignment).
    @Column(name = "training_name")
    private String trainingName;

    // Optional Box Drive (shared folder) link for the assignment brief/uploads.
    // Named repoUrl for consistency with the training's link field.
    @Column(name = "repo_url")
    private String repoUrl;

    // Optional due date, stored as an ISO date string (yyyy-MM-dd) to match the
    // frontend <input type="date"> value and keep JSON serialization trivial.
    @Column(name = "due_date")
    private String dueDate;

    public Assignment() {}

    public Assignment(String title, Integer maxScore, String batch) {
        this.title = title;
        this.maxScore = maxScore;
        this.batch = batch;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getMaxScore() { return maxScore; }
    public void setMaxScore(Integer maxScore) { this.maxScore = maxScore; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getTrainingName() { return trainingName; }
    public void setTrainingName(String trainingName) { this.trainingName = trainingName; }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
}
