package az.qazan.backend.admin.application;

import az.qazan.backend.admin.api.dto.AdminCompanyResponse;
import az.qazan.backend.admin.api.dto.AdminStatsResponse;
import az.qazan.backend.admin.api.dto.AdminUserResponse;
import az.qazan.backend.admin.domain.AdminCoinRepository;
import az.qazan.backend.admin.domain.AdminCompanyRepository;
import az.qazan.backend.admin.domain.AdminUserRepository;
import az.qazan.backend.companies.domain.CompanyStatus;
import az.qazan.backend.loyalty.domain.LoyaltyCardRepository;
import az.qazan.backend.notifications.domain.NotificationRepository;
import az.qazan.backend.notifications.domain.NotificationStatus;
import az.qazan.backend.user.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Baku");
    private static final int SIGNUP_DAYS = 14;

    private final AdminUserRepository users;
    private final AdminCompanyRepository companies;
    private final AdminCoinRepository coins;
    private final LoyaltyCardRepository cards;
    private final NotificationRepository notifications;

    @Transactional(readOnly = true)
    public AdminStatsResponse overview() {
        Instant now = Instant.now();
        Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
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
                        .map(AdminCompanyResponse::from).toList(),
                users.countByCreatedAtGreaterThanEqual(weekAgo),
                companies.countByCreatedAtGreaterThanEqual(weekAgo),
                companies.countByStatus(CompanyStatus.PENDING),
                notifications.countByStatus(NotificationStatus.PENDING),
                users.countByLastLoginAtGreaterThanEqual(now.minus(30, ChronoUnit.DAYS)),
                signups()
        );
    }

    /** New accounts per day for the last 14 days, on the platform's home clock. */
    private List<AdminStatsResponse.DailyCount> signups() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate first = today.minusDays(SIGNUP_DAYS - 1);
        Map<LocalDate, Long> perDay = users
                .createdSince(first.atStartOfDay(ZONE).toInstant()).stream()
                .collect(Collectors.groupingBy(
                        t -> t.atZone(ZONE).toLocalDate(), Collectors.counting()));
        return first.datesUntil(today.plusDays(1))
                .map(d -> new AdminStatsResponse.DailyCount(d, perDay.getOrDefault(d, 0L)))
                .toList();
    }
}
