package az.qazan.backend.reviews.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByUserIdAndCompanyId(UUID userId, UUID companyId);

    long countByCompanyId(UUID companyId);

    @Query("select avg(r.rating) from Review r where r.companyId = :companyId")
    Double averageForCompany(@Param("companyId") UUID companyId);

    /** List with the author's current name (theta-join to User). */
    @Query("""
            select r.id as id, u.fullName as userName, r.rating as rating,
                   r.comment as comment, r.createdAt as createdAt,
                   r.userId as userId
            from Review r, az.qazan.backend.user.domain.User u
            where u.id = r.userId and r.companyId = :companyId
            order by r.createdAt desc
            """)
    List<ReviewView> findViewsByCompany(@Param("companyId") UUID companyId);
}
