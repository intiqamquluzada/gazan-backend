package az.qazan.backend.companies.application;

import az.qazan.backend.common.exception.ConflictException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.api.dto.CreateCompanyRequest;
import az.qazan.backend.companies.api.dto.UpdateCompanyRequest;
import az.qazan.backend.companies.domain.BusinessCategory;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository repo;

    @Transactional(readOnly = true)
    public List<Company> search(BusinessCategory category, String query) {
        return repo.search(category, query == null ? "" : query.trim());
    }

    @Transactional(readOnly = true)
    public List<Company> featured() {
        return repo.findAllByFeaturedTrue();
    }

    @Transactional(readOnly = true)
    public Company getById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Company myCompany(UUID ownerId) {
        return repo.findByOwnerId(ownerId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public Company create(User owner, CreateCompanyRequest req) {
        if (repo.findByOwnerId(owner.getId()).isPresent()) {
            // One business per owner — promote later if a single owner needs many.
            throw new ConflictException(ErrorCode.CONFLICT);
        }
        Company c = Company.builder()
                .name(req.name().trim())
                .tagline(req.tagline())
                .category(req.category())
                .logoEmoji(req.logoEmoji())
                .coverColorHex(req.coverColorHex())
                .address(req.address())
                .rating(0.0)
                .reviewCount(0)
                .featured(false)
                .owner(owner)
                .build();
        return repo.save(c);
    }

    @Transactional
    public Company update(UUID id, UUID requesterId, UpdateCompanyRequest req) {
        Company c = getById(id);
        if (c.getOwner() == null || !c.getOwner().getId().equals(requesterId)) {
            throw new NotFoundException(ErrorCode.NOT_FOUND);
        }
        if (req.name() != null) c.setName(req.name().trim());
        if (req.tagline() != null) c.setTagline(req.tagline());
        if (req.category() != null) c.setCategory(req.category());
        if (req.logoEmoji() != null) c.setLogoEmoji(req.logoEmoji());
        if (req.coverColorHex() != null) c.setCoverColorHex(req.coverColorHex());
        if (req.address() != null) c.setAddress(req.address());
        return c;
    }

    @Transactional
    public void delete(UUID id, UUID requesterId) {
        Company c = getById(id);
        if (c.getOwner() == null || !c.getOwner().getId().equals(requesterId)) {
            throw new NotFoundException(ErrorCode.NOT_FOUND);
        }
        repo.delete(c);
    }
}
