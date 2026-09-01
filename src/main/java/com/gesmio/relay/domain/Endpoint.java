package com.gesmio.relay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "endpoints")
public class Endpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String secret;

    @Column(name = "rate_limit_per_second", nullable = false)
    private int rateLimitPerSecond = 10;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Endpoint() {
    }

    public Endpoint(String name, String url, String secret) {
        this.name = name;
        this.url = url;
        this.secret = secret;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getSecret() {
        return secret;
    }

    public int getRateLimitPerSecond() {
        return rateLimitPerSecond;
    }

    public void setRateLimitPerSecond(int rateLimitPerSecond) {
        this.rateLimitPerSecond = rateLimitPerSecond;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
