package az.qazan.backend.admin.application;

import az.qazan.backend.admin.api.dto.AdminCompanyDetailResponse;
import az.qazan.backend.admin.api.dto.AdminCompanyResponse;
import az.qazan.backend.admin.api.dto.PageResponse;
import az.qazan.backend.admin.domain.AdminCompanyRepository;
import az.qazan.backend.coins.domain.CoinTransactionRepository;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyStatus;
import az.qazan.backend.loyalty.domain.LoyaltyCardRepository;
import az.qazan.backend.nfc.domain.NfcTagRepository;
import az.qazan.backend.notifications.application.NotificationService;
import az.qazan.backend.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCompanyService {

    private final AdminCompanyRepository companies;
    private final AdminAuditService audit;
    private final LoyaltyCardRepository cards;
    private final NfcTagRepository tags;
    private final CoinTransactionRepository ledger;
    private final NotificationService notifications;

    @Transactional(readOnly = true)
    public PageResponse<AdminCompanyResponse> list(String q, CompanyStatus status,
                                                   int page, int size) {
        Page<Company> result = companies.search(q, status, PageRequest.of(
                Math.max(page, 0), Math.max(1, Math.min(size, 100))));
        return PageResponse.of(result, AdminCompanyResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminCompanyDetailResponse detail(UUID companyId) {
        Company c = get(companyId);
        User owner = c.getOwner();
        return new AdminCompanyDetailResponse(
                c.getId(), c.getName(), c.getTagline(), c.getCategory().name(),
                c.isFeatured(), c.getRating(), c.getReviewCount(), c.getAddress(),
                c.getPhone(), c.getCountry(), c.getCurrency(), c.getLogoUrl(),
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getFullName(),
                owner == null ? null : owner.getEmail(),
                c.getCreatedAt(), c.getStatus(),
                cards.countCustomers(companyId),
                tags.countByCompanyId(companyId),
                ledger.totalEarnedAtCompany(companyId),
                ledger.totalSpentAtCompany(companyId));
    }

    @Transactional
    public AdminCompanyResponse setFeatured(UUID actorId, UUID companyId, boolean featured) {
        Company c = get(companyId);
        c.setFeatured(featured);
        audit.record(actorId, "COMPANY_FEATURED", "COMPANY", companyId, String.valueOf(featured));
        return AdminCompanyResponse.from(companies.save(c));
    }

    @Transactional
    public AdminCompanyResponse setStatus(UUID actorId, UUID companyId,
                                          CompanyStatus status, String reason) {
        Company c = get(companyId);
        CompanyStatus previous = c.getStatus();
        c.setStatus(status);
        audit.record(actorId, "COMPANY_STATUS", "COMPANY", companyId,
                previous + " -> " + status + (reason == null ? "" : " : " + reason));
        if (c.getOwner() != null && previous != status) {
            notifyOwner(c, status, reason);
        }
        return AdminCompanyResponse.from(companies.save(c));
    }

    private void notifyOwner(Company c, CompanyStatus status, String reason) {
        String suffix = reason == null || reason.isBlank() ? "" : " " + reason.trim();
        if (status == CompanyStatus.APPROVED) {
            notifications.notifyUser(c.getOwner().getId(), "Biznesiniz təsdiqləndi",
                    c.getName() + " artıq müştərilərə görünür.");
        } else if (status == CompanyStatus.SUSPENDED) {
            notifications.notifyUser(c.getOwner().getId(), "Biznesiniz dayandırıldı",
                    c.getName() + " müvəqqəti olaraq gizlədildi." + suffix);
        }
    }

    private Company get(UUID companyId) {
        return companies.findById(companyId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
    }
}
