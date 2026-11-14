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

    /**
     * The venue's own staff (and admins) may see paused items; everyone
     * else only gets what can be bought.
     */
    @Transactional(readOnly = true)
    public List<CoinReward> listForCaller(UUID companyId, boolean activeOnly,
                                          UUID callerId, boolean admin) {
        if (activeOnly || callerId == null) {
            return listForCompany(companyId, true);
        }
        Company company = companies.findById(companyId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        boolean staff = admin
                || (company.getOwner() != null && company.getOwner().getId().equals(callerId));
        return listForCompany(companyId, !staff);
    }

    /** Owner edits an item; pausing it keeps vouchers already bought redeemable. */
    @Transactional
    public CoinReward update(UUID rewardId, UUID ownerId, String title,
                             String description, Integer coinCost, Boolean active) {
        CoinReward r = rewards.findById(rewardId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        requireOwner(r.getCompany(), ownerId);
        if (title != null && !title.isBlank()) r.setTitle(title.trim());
        if (description != null) r.setDescription(description.isBlank() ? null : description.trim());
        if (coinCost != null) r.setCoinCost(coinCost);
        if (active != null) r.setActive(active);
        return r;
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
