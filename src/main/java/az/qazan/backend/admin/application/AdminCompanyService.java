package az.qazan.backend.admin.application;

import az.qazan.backend.admin.api.dto.AdminCompanyResponse;
import az.qazan.backend.admin.api.dto.PageResponse;
import az.qazan.backend.admin.domain.AdminCompanyRepository;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.domain.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCompanyService {

    private final AdminCompanyRepository companies;

    @Transactional(readOnly = true)
    public PageResponse<AdminCompanyResponse> list(String q, int page, int size) {
        Page<Company> result = companies.search(
                q, PageRequest.of(page, Math.min(size, 100)));
        return PageResponse.of(result, AdminCompanyResponse::from);
    }

    @Transactional
    public AdminCompanyResponse setFeatured(UUID companyId, boolean featured) {
        Company c = companies.findById(companyId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        c.setFeatured(featured);
        return AdminCompanyResponse.from(companies.save(c));
    }
}
