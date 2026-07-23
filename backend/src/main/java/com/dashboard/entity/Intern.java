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

    @Column(name = "talent_id")
    private String talentId;

    private String batch;

    private String track;

    public Intern() {}

    public Intern(String name, String talentId, String batch, String track) {
        this.name = name;
        this.talentId = talentId;
        this.batch = batch;
        this.track = track;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTalentId() { return talentId; }
    public void setTalentId(String talentId) { this.talentId = talentId; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getTrack() { return track; }
    public void setTrack(String track) { this.track = track; }
}
