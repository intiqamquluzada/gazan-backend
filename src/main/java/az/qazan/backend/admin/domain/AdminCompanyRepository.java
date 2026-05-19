package az.qazan.backend.admin.domain;

import az.qazan.backend.companies.domain.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Admin-only read/search view over {@link Company}. Separate from
 * {@code CompanyRepository} so the admin module owns its own queries.
 */
public interface AdminCompanyRepository extends JpaRepository<Company, UUID> {

    @Query("""
        select c from Company c
         where (:q is null or :q = ''
                 or lower(c.name)    like lower(concat('%', :q, '%'))
                 or lower(c.tagline) like lower(concat('%', :q, '%')))
         order by c.featured desc, c.createdAt desc
        """)
    Page<Company> search(@Param("q") String query, Pageable pageable);

    long countByFeaturedTrue();
}
