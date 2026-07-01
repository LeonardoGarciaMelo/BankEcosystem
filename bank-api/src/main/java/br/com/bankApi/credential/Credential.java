package br.com.bankApi.credential;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents the security and authentication identity within the banking system.
 * <p>
 * This entity is strictly isolated from business data (like CPF or balances) to adhere
 * to AppSec best practices. It handles login credentials, account lockouts,
 * and automated auditing timestamps.
 * </p>
 *
 * @version 1.0
 * @since 2026-04-17
 */
@Entity
@Table(name = "credentials")
public class Credential extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID id;

    @Column(unique = true, nullable = false)
    public String username;

    @Column(nullable = false)
    public String password;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    public LocalDateTime lastLoginAt;

    @Column(name = "failed_login_attempts")
    public Short failedLoginAttempts = (short) 0;

    @Column(name = "status", nullable = false)
    public Boolean status = true;

    /**
     * JPA Callback: Automatically sets creation and update timestamps
     * right before the entity is persisted to the database for the first time.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * JPA Callback: Automatically updates the modification timestamp
     * every time the entity is updated in the database.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Retrieves a credential based on the username.
     *
     * @param username The exact username to search for.
     * @return The {@link Credential} entity if found, otherwise {@code null}.
     */
    public static Credential findByUsername(String username) {
        return find("username", username).firstResult();
    }
}
