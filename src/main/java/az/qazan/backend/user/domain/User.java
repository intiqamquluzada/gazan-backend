package az.qazan.backend.user.domain;

import az.qazan.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email", unique = true),
                @Index(name = "idx_users_role", columnList = "role"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    /** BCrypt-hashed password. Never returned to clients. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "phone", length = 32)
    private String phone;

    /** Optional date of birth, collected at registration. */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    /** Optional: business name when the role is {@link Role#BUSINESS_OWNER}. */
    @Column(name = "business_name", length = 120)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "locale", nullable = false, length = 8)
    private AppLocale locale;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public void touchLastLogin() {
        this.lastLoginAt = Instant.now();
    }
}
