package az.qazan.backend.business.application;

import az.qazan.backend.business.api.dto.BusinessCustomerResponse;
import az.qazan.backend.business.domain.BusinessCustomerRepository;
import az.qazan.backend.business.domain.CoinCustomerProjection;
import az.qazan.backend.companies.application.CompanyService;
import az.qazan.backend.companies.domain.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessCustomersService {

    private final CompanyService companies;
    private final BusinessCustomerRepository repo;

    @Transactional(readOnly = true)
    public List<BusinessCustomerResponse> customersForOwner(UUID ownerId) {
        Company company = companies.myCompany(ownerId);
        // Stamp-card holders and coin-only customers, one row per person.
        Map<UUID, CoinCustomerProjection> coins = new LinkedHashMap<>();
        for (CoinCustomerProjection c : repo.coinCustomersOfCompany(company.getId())) {
            coins.put(c.getUserId(), c);
        }
        List<BusinessCustomerResponse> out = new ArrayList<>();
        repo.customersOfCompany(company.getId()).forEach(p -> {
            CoinCustomerProjection c = coins.remove(p.getUserId());
            out.add(new BusinessCustomerResponse(
                    p.getUserId(),
                    p.getFullName(),
                    p.getPhone(),
                    p.getTotalStamps(),
                    p.getRewardsClaimed(),
                    p.getCardCount(),
                    latest(p.getLastActivityAt(), c == null ? null : c.getLastActivityAt()),
                    c == null ? 0 : c.getBalance()));
        });
        coins.values().forEach(c -> out.add(new BusinessCustomerResponse(
                c.getUserId(), c.getFullName(), c.getPhone(),
                0, 0, 0, c.getLastActivityAt(), c.getBalance())));
        out.sort(Comparator.comparing(BusinessCustomerResponse::lastActivityAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    private static Instant latest(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
