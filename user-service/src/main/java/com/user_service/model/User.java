package com.user_service.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.user_service.enums.UserStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_auth_id", columnList = "auth_id"),
        @Index(name = "idx_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "contact_no", length = 20)
    private String contactNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "auth_id", unique = true, nullable = false)
    private String authId; // Keycloak user ID

    @Column(name = "identification_number", unique = true, nullable = false)
    private String identificationNumber;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    private Profile profile;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Version
    private Long version; // Optimistic locking

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    /**
     * Flag to force user to reset password on next login
     * Set to true by admin or security policies
     */
    @Column(name = "password_reset_required")
    @Builder.Default
    private Boolean passwordResetRequired = false;

    /**
     * Account locked timestamp (for security lockouts)
     */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    /**
     * Failed login attempts counter
     */
    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    /**
     * Last failed login attempt timestamp
     */
    @Column(name = "last_failed_login_at")
    private LocalDateTime lastFailedLoginAt;

    /**
     * Last successful login timestamp
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * Last login IP address
     */
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }

    public boolean isLocked() {
        return this.lockedAt != null;
    }

    public void lockAccount() {
        this.lockedAt = LocalDateTime.now();
        this.status = UserStatus.LOCKED;
    }

    public void unlockAccount() {
        this.lockedAt = null;
        this.failedLoginAttempts = 0;
        this.status = UserStatus.ACTIVE;
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
        this.lastFailedLoginAt = LocalDateTime.now();
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;
    }

    public void recordSuccessfulLogin(String ipAddress) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
        this.resetFailedLoginAttempts();
    }
}