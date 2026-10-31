package com.example.galdcup.statistics;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_signup_events")
@Getter
public class SignupEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SignupEvent() { }

    public SignupEvent(OffsetDateTime createdAt) {
        this.createdAt = java.util.Objects.requireNonNull(createdAt);
    }
}
