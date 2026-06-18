package com.danovich.platform.login.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String email = "";

    /** Null for social accounts (Google/GitHub), which have no local password. */
    @Column(name = "password_hash")
    private String passwordHash;

    /** LOCAL, GOOGLE, or GITHUB. */
    @Column(nullable = false)
    private String provider = "LOCAL";

    /** The provider's user id (sub) for social accounts; null for LOCAL. */
    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected User() {
    }

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /** Social account constructor (no password). */
    public User(String email, String provider, String providerId) {
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
