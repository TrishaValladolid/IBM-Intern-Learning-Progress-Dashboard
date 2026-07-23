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
}
