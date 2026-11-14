package az.qazan.backend.user.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findFirstByPhoneOrderByCreatedAtAsc(String phone);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, UUID id);

    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByAppleSub(String appleSub);

    List<User> findAllByStaffCompanyIdOrderByCreatedAtDesc(UUID staffCompanyId);

    /**
     * Serializes everything that checks a coin balance and then writes to
     * the ledger for this user. The ledger is insert-only, so optimistic
     * versioning cannot protect it.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(@Param("id") UUID id);
}
