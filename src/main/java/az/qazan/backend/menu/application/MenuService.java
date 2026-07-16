package az.qazan.backend.menu.application;

import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.menu.api.dto.MenuItemRequest;
import az.qazan.backend.menu.domain.MenuItem;
import az.qazan.backend.menu.domain.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemRepository items;
    private final CompanyRepository companies;

    @Transactional(readOnly = true)
    public List<MenuItem> forCompany(UUID companyId) {
        return items.findAllByCompanyIdOrderBySortOrderAscCreatedAtAsc(companyId);
    }

    @Transactional
    public MenuItem create(UUID companyId, MenuItemRequest req) {
        Company company = companies.findById(companyId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));
        MenuItem m = MenuItem.builder()
                .company(company)
                .category(req.category().trim())
                .name(req.name().trim())
                .description(blankToNull(req.description()))
                .price(req.price())
                .sortOrder(req.sortOrder() == null ? 0 : req.sortOrder())
                .build();
        return items.save(m);
    }

    @Transactional
    public MenuItem update(UUID id, MenuItemRequest req) {
        MenuItem m = items.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));
        if (req.category() != null) m.setCategory(req.category().trim());
        if (req.name() != null) m.setName(req.name().trim());
        if (req.description() != null) m.setDescription(blankToNull(req.description()));
        if (req.price() != null) m.setPrice(req.price());
        if (req.sortOrder() != null) m.setSortOrder(req.sortOrder());
        return m;
    }

    @Transactional
    public void delete(UUID id) {
        items.deleteById(id);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
