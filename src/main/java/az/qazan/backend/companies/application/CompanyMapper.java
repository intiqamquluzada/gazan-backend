package az.qazan.backend.companies.application;

import az.qazan.backend.companies.api.dto.CompanyResponse;
import az.qazan.backend.companies.domain.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponse toResponse(Company c) {
        return new CompanyResponse(
                c.getId(),
                c.getName(),
                c.getTagline(),
                c.getCategory(),
                c.getLogoEmoji(),
                c.getCoverColorHex(),
                c.getAddress(),
                c.getRating(),
                c.getReviewCount(),
                c.isFeatured(),
                c.getPhone(),
                c.getInstagram(),
                c.getWorkingHours(),
                c.getLatitude(),
                c.getLongitude(),
                c.getAmenities(),
                c.getPhotoUrls(),
                c.getMenuUrl()
        );
    }
}
