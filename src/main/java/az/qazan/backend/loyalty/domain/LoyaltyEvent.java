package az.qazan.backend.loyalty.domain;

import az.qazan.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "loyalty_events",
        indexes = {
                @Index(name = "idx_event_card", columnList = "card_id"),
                @Index(name = "idx_event_created", columnList = "created_at"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyEvent extends BaseEntity {

    public enum Type { STAMP_ADDED, REWARD_CLAIMED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private LoyaltyCard card;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private Type type;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "note", length = 240)
    private String note;
}
