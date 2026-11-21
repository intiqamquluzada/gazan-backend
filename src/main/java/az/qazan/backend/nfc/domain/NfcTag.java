package az.qazan.backend.nfc.domain;

import az.qazan.backend.common.audit.BaseEntity;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.loyalty.domain.LoyaltyProgram;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** One physical NFC card handed to a venue. */
@Entity
@Table(name = "nfc_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfcTag extends BaseEntity {

    public enum Kind { UNASSIGNED, STAMP, POINTS, SPEND }

    public enum Status { ACTIVE, LOST, REVOKED, SUSPENDED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** The campaign this card stamps (STAMP cards only — each campaign has its own card). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private LoyaltyProgram program;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private Kind kind;

    @Column(name = "label", length = 80)
    private String label;

    /** Secret written onto the tag; never shown after provisioning. */
    @Column(name = "token", nullable = false, length = 64, unique = true)
    private String token;

    /** Learned on the first tap, then required to match (blocks naive copies). */
    @Column(name = "tag_uid", length = 32)
    private String tagUid;

    @Column(name = "key_version", nullable = false)
    private int keyVersion;

    @Column(name = "last_counter", nullable = false)
    private long lastCounter;

    @Column(name = "points_value", nullable = false)
    private int pointsValue;

    @Column(name = "stamps_value", nullable = false)
    private int stampsValue;

    @Column(name = "cooldown_sec", nullable = false)
    private int cooldownSec;

    @Column(name = "daily_cap_per_user", nullable = false)
    private int dailyCapPerUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status;

    @Column(name = "provisioned_at")
    private Instant provisionedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean isUsable() {
        return status == Status.ACTIVE;
    }
}
