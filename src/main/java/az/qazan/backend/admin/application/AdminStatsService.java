package az.qazan.backend.admin.application;

import az.qazan.backend.admin.api.dto.AdminCompanyResponse;
import az.qazan.backend.admin.api.dto.AdminStatsResponse;
import az.qazan.backend.admin.api.dto.AdminUserResponse;
import az.qazan.backend.admin.domain.AdminCoinRepository;
import az.qazan.backend.admin.domain.AdminCompanyRepository;
import az.qazan.backend.admin.domain.AdminUserRepository;
import az.qazan.backend.loyalty.domain.LoyaltyCardRepository;
import az.qazan.backend.user.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final AdminUserRepository users;
    private final AdminCompanyRepository companies;
    private final AdminCoinRepository coins;
    private final LoyaltyCardRepository cards;

    @Transactional(readOnly = true)
    public AdminStatsResponse overview() {
        var recentUsersPage = users.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")));
        var recentCompaniesPage = companies.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")));

        return new AdminStatsResponse(
                users.count(),
                users.countByRole(Role.CUSTOMER),
                users.countByRole(Role.BUSINESS_OWNER),
                users.countByRole(Role.ADMIN),
                companies.count(),
                companies.countByFeaturedTrue(),
                cards.count(),
                coins.circulating(),
                coins.totalEarned(),
                coins.totalSpent(),
                coins.count(),
                recentUsersPage.getContent().stream()
                        .map(AdminUserResponse::from).toList(),
                recentCompaniesPage.getContent().stream()
                        .map(AdminCompanyResponse::from).toList()
        );
    }
}
