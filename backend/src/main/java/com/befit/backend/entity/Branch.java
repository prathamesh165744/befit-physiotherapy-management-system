package com.befit.backend.entity;

import jakarta.persistence.*;

/** One row per clinic branch. The id is the same code string already used everywhere (e.g. "KARVE-ROAD"). */
@Entity
@Table(name = "branches")
public class Branch {
    @Id
    @Column(length = 50)
    private String id;
    private String name;
    @Column(name = "is_active")
    private boolean active = true;

    public Branch() {}
    public Branch(String id, String name) { this.id = id; this.name = name; }

    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; } public void setActive(boolean active) { this.active = active; }
}
