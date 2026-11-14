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

    @Query("""
            select r.id as id, r.rating as rating, r.comment as comment,
                   r.createdAt as createdAt, u.fullName as userName,
                   c.id as companyId, c.name as companyName,
                   (select count(rr) from ReviewReport rr where rr.reviewId = r.id) as reportCount
            from Review r, az.qazan.backend.user.domain.User u,
                 az.qazan.backend.companies.domain.Company c
            where u.id = r.userId and c.id = r.companyId
            order by r.createdAt desc
            """)
    List<AdminReviewView> findLatestForAdmin(
            org.springframework.data.domain.Pageable page);

    /** Only reviews someone reported, most reported first. */
    @Query("""
            select r.id as id, r.rating as rating, r.comment as comment,
                   r.createdAt as createdAt, u.fullName as userName,
                   c.id as companyId, c.name as companyName,
                   (select count(rr) from ReviewReport rr where rr.reviewId = r.id) as reportCount
            from Review r, az.qazan.backend.user.domain.User u,
                 az.qazan.backend.companies.domain.Company c
            where u.id = r.userId and c.id = r.companyId
              and exists (select 1 from ReviewReport rr2 where rr2.reviewId = r.id)
            order by (select count(rr3) from ReviewReport rr3 where rr3.reviewId = r.id) desc,
                     r.createdAt desc
            """)
    List<AdminReviewView> findReportedForAdmin(
            org.springframework.data.domain.Pageable page);

    /**
     * A "verified visit": the user got at least one stamp on a card of this
     * company, or earned coins there. Only such users may review it.
     */
    @Query(value = """
            select exists(
                     select 1 from loyalty_events e
                     join loyalty_cards c on c.id = e.card_id
                     where c.user_id = :userId and c.company_id = :companyId
                       and e.type = 'STAMP_ADDED')
                or exists(
                     select 1 from coin_transactions t
                     where t.user_id = :userId and t.company_id = :companyId
                       and t.amount > 0)
            """, nativeQuery = true)
    boolean hasVisited(@Param("userId") UUID userId,
                       @Param("companyId") UUID companyId);

    @Query(value = "select exists(select 1 from companies where id = :companyId and owner_id = :userId)",
            nativeQuery = true)
    boolean isOwner(@Param("userId") UUID userId,
                    @Param("companyId") UUID companyId);

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
