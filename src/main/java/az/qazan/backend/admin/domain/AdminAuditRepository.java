package az.qazan.backend.admin.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminAuditRepository extends JpaRepository<AdminAuditEntry, UUID> {

    Page<AdminAuditEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
