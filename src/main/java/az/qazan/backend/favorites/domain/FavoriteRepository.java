package az.qazan.backend.favorites.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    boolean existsByUserIdAndCompanyId(UUID userId, UUID companyId);

    void deleteByUserIdAndCompanyId(UUID userId, UUID companyId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from Favorite f where f.userId = :userId")
    void deleteAllOfUser(@Param("userId") UUID userId);

    @Query("select f.companyId from Favorite f where f.userId = :userId")
    List<UUID> findCompanyIdsByUserId(@Param("userId") UUID userId);
}
