package az.qazan.backend.favorites.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    boolean existsByUserIdAndCompanyId(UUID userId, UUID companyId);

    void deleteByUserIdAndCompanyId(UUID userId, UUID companyId);

    @Query("select f.companyId from Favorite f where f.userId = :userId")
    List<UUID> findCompanyIdsByUserId(@Param("userId") UUID userId);
}
