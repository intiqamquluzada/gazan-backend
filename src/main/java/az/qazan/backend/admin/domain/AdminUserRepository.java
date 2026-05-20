package az.qazan.backend.admin.domain;

import az.qazan.backend.user.domain.Role;
import az.qazan.backend.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Admin-only read/search view over {@link User}.
 *
 * <p>A second Spring Data repository for the same entity — kept separate
 * from {@code UserRepository} so the admin module owns its own queries
 * and never widens the surface of another module's repository.
 */
public interface AdminUserRepository extends JpaRepository<User, UUID> {

    @Query("""
        select u from User u
         where (:q is null or :q = ''
                 or lower(u.fullName) like lower(concat('%', :q, '%'))
                 or lower(u.email)    like lower(concat('%', :q, '%')))
           and (:role is null or u.role = :role)
         order by u.createdAt desc
        """)
    Page<User> search(
            @Param("q") String query,
            @Param("role") Role role,
            Pageable pageable
    );

    long countByRole(Role role);
}
