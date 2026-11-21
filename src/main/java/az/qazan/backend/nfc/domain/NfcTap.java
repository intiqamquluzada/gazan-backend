package az.qazan.backend.nfc.domain;

import az.qazan.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Audit row for every tap — accepted or rejected. */
@Entity
@Table(name = "nfc_taps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfcTap extends BaseEntity {

    public enum Result {
        OK, UNKNOWN_TAG, REVOKED, WRONG_UID, REPLAY, COOLDOWN, DAILY_CAP,
        OUT_OF_RANGE, MODE_OFF, DUPLICATE, MISSING_UID, UNASSIGNED,
        NOT_APPROVED, NO_REWARDS, TOO_OLD
    }

    @Column(name = "tag_id")
    private UUID tagId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "counter", nullable = false)
    private long counter;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 24)
    private Result result;

    @Column(name = "points_delta", nullable = false)
    private int pointsDelta;

    @Column(name = "stamps_delta", nullable = false)
    private int stampsDelta;

    @Column(name = "distance_m")
    private Integer distanceM;

    @Column(name = "device_id", length = 80)
    private String deviceId;

    @Column(name = "client_tap_id", length = 64)
    private String clientTapId;

    @Column(name = "suspicious", nullable = false)
    private boolean suspicious;

    @Column(name = "note", length = 255)
    private String note;
}
