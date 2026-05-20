package az.qazan.backend.business.application;

import az.qazan.backend.business.api.dto.BusinessCustomerResponse;
import az.qazan.backend.business.domain.BusinessCustomerRepository;
import az.qazan.backend.companies.application.CompanyService;
import az.qazan.backend.companies.domain.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessCustomersService {

    private final CompanyService companies;
    private final BusinessCustomerRepository repo;

    @Transactional(readOnly = true)
    public List<BusinessCustomerResponse> customersForOwner(UUID ownerId) {
        Company company = companies.myCompany(ownerId);
        return repo.customersOfCompany(company.getId()).stream()
                .map(p -> new BusinessCustomerResponse(
                        p.getUserId(),
                        p.getFullName(),
                        p.getPhone(),
                        p.getTotalStamps(),
                        p.getRewardsClaimed(),
                        p.getCardCount(),
                        p.getLastActivityAt()))
                .toList();
    }
}
