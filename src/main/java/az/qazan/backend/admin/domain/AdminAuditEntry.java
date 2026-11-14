package az.qazan.backend.admin.domain;

import az.qazan.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Who did what in the admin panel. Rows are only ever added. */
@Entity
@Table(name = "admin_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuditEntry extends BaseEntity {

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 254)
    private String actorEmail;

    @Column(name = "action", nullable = false, length = 48)
    private String action;

    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "detail", length = 500)
    private String detail;
}
