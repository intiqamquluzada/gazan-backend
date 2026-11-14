package az.qazan.backend.nfc.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NfcTagRepository extends JpaRepository<NfcTag, UUID> {

    /** Taps of one card run one at a time so limits cannot be raced. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from NfcTag t where t.token = :token")
    Optional<NfcTag> lockByToken(@Param("token") String token);

    long countByCompanyId(UUID companyId);

    /** Tap path + DTO mapping both need company/program loaded. */
    @Query("""
            select t from NfcTag t
            join fetch t.company left join fetch t.program
            where t.token = :token
            """)
    Optional<NfcTag> findByTokenWithRefs(@Param("token") String token);

    @Query("""
            select t from NfcTag t
            join fetch t.company left join fetch t.program
            where t.id = :id
            """)
    Optional<NfcTag> findByIdWithRefs(@Param("id") UUID id);

    @Query("""
            select t from NfcTag t
            join fetch t.company c left join fetch t.program
            where c.id = :companyId
            order by t.createdAt asc
            """)
    List<NfcTag> findAllForCompany(@Param("companyId") UUID companyId);

    @Query("""
            select t from NfcTag t
            join fetch t.company left join fetch t.program
            order by t.createdAt desc
            """)
    List<NfcTag> findAllWithRefs();
}
