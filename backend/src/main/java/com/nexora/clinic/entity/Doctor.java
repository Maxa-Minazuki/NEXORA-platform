package com.nexora.clinic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;

    @Column(length = 120)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(length = 20)
    private String room;

    @Column(length = 500)
    private String bio;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
    @OrderBy("dayOfWeek")
    private List<DoctorSchedule> schedules = new ArrayList<>();

    protected Doctor() {
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public Specialty getSpecialty() { return specialty; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRoom() { return room; }
    public String getBio() { return bio; }
    public boolean isActive() { return active; }
    public List<DoctorSchedule> getSchedules() { return schedules; }
}
