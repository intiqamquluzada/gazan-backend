package az.qazan.backend.companies.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByOwnerId(UUID ownerId);

    List<Company> findAllByFeaturedTrue();

    @Query("""
        select c from Company c
         where (:category is null or c.category = :category)
           and (:q is null or :q = ''
                 or lower(c.name) like lower(concat('%', :q, '%'))
                 or lower(c.tagline) like lower(concat('%', :q, '%')))
         order by c.featured desc, c.rating desc nulls last, c.name asc
        """)
    List<Company> search(
            @Param("category") BusinessCategory category,
            @Param("q") String query
    );
}
