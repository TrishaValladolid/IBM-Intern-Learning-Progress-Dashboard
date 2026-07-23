package com.dashboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "intern")
public class Intern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Column(name = "employee_id")
    private String employeeId;

    private String batch;

    private String track;

    public Intern() {}

    public Intern(String name, String employeeId, String batch, String track) {
        this.name = name;
        this.employeeId = employeeId;
        this.batch = batch;
        this.track = track;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getTrack() { return track; }
    public void setTrack(String track) { this.track = track; }
}
