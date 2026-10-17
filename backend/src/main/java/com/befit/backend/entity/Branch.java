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
    @Column(columnDefinition = "TEXT") private String address;
    @Column(length = 30) private String phone;
    private String email;
    @Column(name = "is_active")
    private boolean active = true;

    public Branch() {}
    public Branch(String id, String name) { this.id = id; this.name = name; }
    public Branch(String id, String name, String address, String phone, String email) {
        this.id = id; this.name = name; this.address = address; this.phone = phone; this.email = email;
    }

    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getAddress() { return address; } public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public boolean isActive() { return active; } public void setActive(boolean active) { this.active = active; }
}
