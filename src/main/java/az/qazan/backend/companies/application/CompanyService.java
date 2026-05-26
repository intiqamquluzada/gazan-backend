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
        if (req.phone() != null) c.setPhone(req.phone());
        if (req.instagram() != null) c.setInstagram(req.instagram());
        if (req.workingHours() != null) c.setWorkingHours(req.workingHours());
        if (req.latitude() != null) c.setLatitude(req.latitude());
        if (req.longitude() != null) c.setLongitude(req.longitude());
        if (req.amenities() != null) c.setAmenities(req.amenities());
        if (req.photoUrls() != null) c.setPhotoUrls(req.photoUrls());
        if (req.menuUrl() != null) c.setMenuUrl(req.menuUrl());
        if (req.coinRate() != null) c.setCoinRate(req.coinRate());
        if (req.logoUrl() != null) c.setLogoUrl(req.logoUrl());
        // Per-language overrides (each independent; null = no change,
        // empty string clears the translation back to AZ fallback).
        if (req.nameEn() != null) c.setNameEn(blankToNull(req.nameEn()));
        if (req.nameRu() != null) c.setNameRu(blankToNull(req.nameRu()));
        if (req.nameTr() != null) c.setNameTr(blankToNull(req.nameTr()));
        if (req.taglineEn() != null) c.setTaglineEn(blankToNull(req.taglineEn()));
        if (req.taglineRu() != null) c.setTaglineRu(blankToNull(req.taglineRu()));
        if (req.taglineTr() != null) c.setTaglineTr(blankToNull(req.taglineTr()));
        return c;
    }

    private static String blankToNull(String s) {
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
