package com.dashboard.entity;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

/**
 * A single training an intern completed during their internship
 * (e.g. Java, Ionic, Japanese Language, Spring Boot). Some trainings
 * require a GitHub repository submission; others do not, so repoUrl is optional.
 *
 * Relational design: each Training row belongs to one Intern via a foreign key,
 * mirroring the existing Submission/Attendance -> Intern relationship. GitHub
 * links live here, never as columns on the intern table.
 */
@Entity
@Table(name = "training")
public class Training {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intern_id", nullable = false)
    @JsonbTransient
    private Intern intern;

    @Column(name = "training_name", nullable = false)
    private String trainingName;

    // Optional GitHub repository URL — null when the training needs no submission.
    @Column(name = "repo_url")
    private String repoUrl;

    public Training() {}

    public Training(Intern intern, String trainingName, String repoUrl) {
        this.intern = intern;
        this.trainingName = trainingName;
        this.repoUrl = repoUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Intern getIntern() { return intern; }
    public void setIntern(Intern intern) { this.intern = intern; }

    public String getTrainingName() { return trainingName; }
    public void setTrainingName(String trainingName) { this.trainingName = trainingName; }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
}
