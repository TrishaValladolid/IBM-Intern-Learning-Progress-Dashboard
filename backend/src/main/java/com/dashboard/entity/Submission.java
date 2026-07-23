package com.dashboard.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "submission")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intern_id", nullable = false)
    private Intern intern;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    private Integer score;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    public enum Status { PENDING, SUBMITTED, GRADED }

    public Submission() {}

    public Submission(Intern intern, Assignment assignment, Integer score, Status status) {
        this.intern = intern;
        this.assignment = assignment;
        this.score = score;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Intern getIntern() { return intern; }
    public void setIntern(Intern intern) { this.intern = intern; }

    public Assignment getAssignment() { return assignment; }
    public void setAssignment(Assignment assignment) { this.assignment = assignment; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
