package az.qazan.backend.coins.application;

import az.qazan.backend.coins.api.dto.CoinRewardCatalogResponse;
import az.qazan.backend.coins.domain.CoinReward;
import az.qazan.backend.coins.domain.CoinRewardRepository;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.common.exception.UnauthorizedException;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoinRewardService {

    private final CoinRewardRepository rewards;
    private final CompanyRepository companies;

    @Transactional(readOnly = true)
    public List<CoinReward> listForCompany(UUID companyId, boolean activeOnly) {
        return activeOnly
                ? rewards.findAllByCompanyIdAndActiveTrueOrderByCoinCostAsc(companyId)
                : rewards.findAllByCompanyIdOrderByCoinCostAsc(companyId);
    }

    /** Platform-wide active reward catalog for the customer wallet. */
    @Transactional(readOnly = true)
    public List<CoinRewardCatalogResponse> catalog() {
        return rewards.findAllActiveWithCompany().stream()
                .map(CoinRewardCatalogResponse::from)
                .toList();
    }

    @Transactional
    public CoinReward create(UUID companyId, UUID ownerId,
                             String title, String description, int coinCost) {
        Company company = companies.findById(companyId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        requireOwner(company, ownerId);
        return rewards.save(CoinReward.builder()
                .company(company)
                .title(title)
                .description(description)
                .coinCost(coinCost)
                .active(true)
                .build());
    }

    @Transactional
    public void delete(UUID rewardId, UUID ownerId) {
        CoinReward r = rewards.findById(rewardId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        requireOwner(r.getCompany(), ownerId);
        rewards.delete(r);
    }

    private void requireOwner(Company company, UUID ownerId) {
        if (company.getOwner() == null
                || !company.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException(ErrorCode.FORBIDDEN);
        }
    }
}
