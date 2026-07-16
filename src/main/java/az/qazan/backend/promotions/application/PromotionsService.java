package az.qazan.backend.promotions.application;

import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.promotions.api.dto.AdminPromotionRequest;
import az.qazan.backend.promotions.domain.Promotion;
import az.qazan.backend.promotions.domain.PromotionRepository;
import az.qazan.backend.promotions.domain.Story;
import az.qazan.backend.promotions.domain.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionsService {

    private final StoryRepository stories;
    private final PromotionRepository promotions;
    private final CompanyRepository companies;

    @Transactional(readOnly = true)
    public Map<UUID, List<Story>> storyGroups() {
        Map<UUID, List<Story>> grouped = new LinkedHashMap<>();
        for (Story s : stories.findAllByActiveTrueOrderByCompanyIdAscSortOrderAsc()) {
            grouped.computeIfAbsent(s.getCompany().getId(), k -> new java.util.ArrayList<>())
                    .add(s);
        }
        return grouped;
    }

    @Transactional(readOnly = true)
    public List<Story> storiesForCompany(UUID companyId) {
        return stories.findAllByCompanyIdAndActiveTrueOrderBySortOrderAsc(companyId);
    }

    @Transactional(readOnly = true)
    public List<Promotion> activePromotions() {
        return promotions.findAllByActiveTrueOrderBySortOrderAsc();
    }

    // ── Admin CRUD ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Promotion> allForAdmin() {
        return promotions.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public Promotion create(AdminPromotionRequest req) {
        Promotion p = Promotion.builder()
                .company(resolveCompany(req.companyId()))
                .tag(req.tag().trim())
                .title(req.title().trim())
                .subtitle(trimOrNull(req.subtitle()))
                .emoji(trimOrNull(req.emoji()))
                .gradientStartHex(req.gradientStartHex() == null
                        ? 0xFF6C2BD9L : req.gradientStartHex())
                .gradientEndHex(req.gradientEndHex() == null
                        ? 0xFF3D1486L : req.gradientEndHex())
                .cta(trimOrNull(req.cta()))
                .active(req.active() == null || req.active())
                .endsAt(req.endsAt())
                .sortOrder(req.sortOrder() == null ? 0 : req.sortOrder())
                .build();
        return promotions.save(p);
    }

    @Transactional
    public Promotion update(UUID id, AdminPromotionRequest req) {
        Promotion p = promotions.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));
        if (req.companyId() != null) p.setCompany(resolveCompany(req.companyId()));
        if (req.tag() != null) p.setTag(req.tag().trim());
        if (req.title() != null) p.setTitle(req.title().trim());
        if (req.subtitle() != null) p.setSubtitle(trimOrNull(req.subtitle()));
        if (req.emoji() != null) p.setEmoji(trimOrNull(req.emoji()));
        if (req.gradientStartHex() != null) p.setGradientStartHex(req.gradientStartHex());
        if (req.gradientEndHex() != null) p.setGradientEndHex(req.gradientEndHex());
        if (req.cta() != null) p.setCta(trimOrNull(req.cta()));
        if (req.active() != null) p.setActive(req.active());
        if (req.endsAt() != null) p.setEndsAt(req.endsAt());
        if (req.sortOrder() != null) p.setSortOrder(req.sortOrder());
        return p;
    }

    @Transactional
    public Promotion setActive(UUID id, boolean active) {
        Promotion p = promotions.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));
        p.setActive(active);
        return p;
    }

    @Transactional
    public void delete(UUID id) {
        promotions.deleteById(id);
    }

    private Company resolveCompany(UUID companyId) {
        if (companyId == null) return null;
        return companies.findById(companyId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
