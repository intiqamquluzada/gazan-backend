package az.qazan.backend.nfc.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NfcTapRepository extends JpaRepository<NfcTap, UUID> {

    Optional<NfcTap> findByUserIdAndClientTapId(UUID userId, String clientTapId);

    long countByCompanyIdAndCreatedAtGreaterThanEqual(UUID companyId, Instant since);

    long countByCompanyIdAndResultAndCreatedAtGreaterThanEqual(
            UUID companyId, NfcTap.Result result, Instant since);

    @Query("""
            select t from NfcTap t
            where t.userId = :userId and t.tagId = :tagId and t.result = :result
            order by t.createdAt desc
            """)
    List<NfcTap> lastWithResult(@Param("userId") UUID userId,
                                @Param("tagId") UUID tagId,
                                @Param("result") NfcTap.Result result,
                                Pageable page);

    @Query("""
            select count(t) from NfcTap t
            where t.userId = :userId and t.tagId = :tagId and t.result = :result
              and t.createdAt >= :since
            """)
    long countWithResultSince(@Param("userId") UUID userId,
                              @Param("tagId") UUID tagId,
                              @Param("result") NfcTap.Result result,
                              @Param("since") Instant since);

    @Query("""
            select t from NfcTap t
            where t.companyId = :companyId and t.createdAt >= :since
            order by t.createdAt desc
            """)
    List<NfcTap> forCompanySince(@Param("companyId") UUID companyId,
                                 @Param("since") Instant since,
                                 Pageable page);

    @Query("""
            select t from NfcTap t
            where (:suspiciousOnly = false or t.suspicious = true)
            order by t.createdAt desc
            """)
    List<NfcTap> latest(@Param("suspiciousOnly") boolean suspiciousOnly,
                        Pageable page);
}
