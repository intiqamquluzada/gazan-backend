package az.qazan.backend.admin.application;

import az.qazan.backend.admin.api.dto.AdminAuditResponse;
import az.qazan.backend.admin.api.dto.PageResponse;
import az.qazan.backend.admin.domain.AdminAuditEntry;
import az.qazan.backend.admin.domain.AdminAuditRepository;
import az.qazan.backend.admin.domain.AdminUserRepository;
import az.qazan.backend.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private static final int MAX_DETAIL = 500;

    private final AdminAuditRepository entries;
    private final AdminUserRepository users;

    /** Joins the caller's transaction, so the note and the change commit together. */
    @Transactional
    public void record(UUID actorId, String action, String targetType,
                       Object targetId, String detail) {
        entries.save(AdminAuditEntry.builder()
                .actorId(actorId)
                .actorEmail(users.findById(actorId).map(User::getEmail).orElse(null))
                .action(action)
                .targetType(targetType)
                .targetId(targetId == null ? null : targetId.toString())
                .detail(detail == null || detail.length() <= MAX_DETAIL
                        ? detail : detail.substring(0, MAX_DETAIL))
                .build());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAuditResponse> list(int page, int size) {
        return PageResponse.of(
                entries.findAllByOrderByCreatedAtDesc(PageRequest.of(
                        Math.max(page, 0), Math.max(1, Math.min(size, 100)))),
                AdminAuditResponse::from);
    }
}
