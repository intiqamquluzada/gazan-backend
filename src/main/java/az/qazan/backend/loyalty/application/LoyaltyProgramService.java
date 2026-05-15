package az.qazan.backend.loyalty.application;

import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.loyalty.api.dto.CreateProgramRequest;
import az.qazan.backend.loyalty.api.dto.UpdateProgramRequest;
import az.qazan.backend.loyalty.domain.LoyaltyProgram;
import az.qazan.backend.loyalty.domain.LoyaltyProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoyaltyProgramService {

    private final LoyaltyProgramRepository programs;
    private final CompanyRepository companies;

    @Transactional(readOnly = true)
    public List<LoyaltyProgram> listForCompany(UUID companyId, boolean activeOnly) {
        return activeOnly
                ? programs.findAllByCompanyIdAndActiveTrue(companyId)
                : programs.findAllByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public LoyaltyProgram getById(UUID id) {
        return programs.findById(id)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public LoyaltyProgram create(UUID companyId, UUID requesterId, CreateProgramRequest req) {
        Company company = companies.findById(companyId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        if (company.getOwner() == null || !company.getOwner().getId().equals(requesterId)) {
            throw new NotFoundException(ErrorCode.NOT_FOUND);
        }
        LoyaltyProgram p = LoyaltyProgram.builder()
                .company(company)
                .title(req.title().trim())
                .description(req.description())
                .stampsRequired(req.stampsRequired())
                .rewardType(req.rewardType())
                .rewardValue(req.rewardValue())
                .rewardItem(req.rewardItem() == null ? "məhsul" : req.rewardItem().trim())
                .active(true)
                .build();
        return programs.save(p);
    }

    @Transactional
    public LoyaltyProgram update(UUID programId, UUID requesterId, UpdateProgramRequest req) {
        LoyaltyProgram p = getById(programId);
        Company company = p.getCompany();
        if (company.getOwner() == null || !company.getOwner().getId().equals(requesterId)) {
            throw new NotFoundException(ErrorCode.NOT_FOUND);
        }
        if (req.title() != null) p.setTitle(req.title().trim());
        if (req.description() != null) p.setDescription(req.description());
        if (req.stampsRequired() != null) p.setStampsRequired(req.stampsRequired());
        if (req.rewardType() != null) p.setRewardType(req.rewardType());
        if (req.rewardValue() != null) p.setRewardValue(req.rewardValue());
        if (req.rewardItem() != null) p.setRewardItem(req.rewardItem().trim());
        if (req.active() != null) p.setActive(req.active());
        return p;
    }

    @Transactional
    public void delete(UUID programId, UUID requesterId) {
        LoyaltyProgram p = getById(programId);
        Company company = p.getCompany();
        if (company.getOwner() == null || !company.getOwner().getId().equals(requesterId)) {
            throw new NotFoundException(ErrorCode.NOT_FOUND);
        }
        programs.delete(p);
    }
}
