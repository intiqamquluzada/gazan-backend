package az.qazan.backend.admin.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** One box that finds people and businesses by name, email, phone or id. */
@RestController
@RequestMapping("/api/v1/admin/search")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminSearchController {

    private static final int LIMIT = 8;

    private final EntityManager em;

    public record SearchHit(String type, UUID id, String title, String subtitle, String badge) {}

    public record SearchResult(String query, List<SearchHit> users, List<SearchHit> companies) {}

    @Operation(summary = "Global search across users and businesses")
    @GetMapping
    @Transactional(readOnly = true)
    public SearchResult search(@RequestParam String q) {
        String query = q == null ? "" : q.trim();
        if (query.length() < 2) return new SearchResult(query, List.of(), List.of());
        UUID id = parseId(query);
        String like = "%" + query.toLowerCase() + "%";
        String digits = query.replaceAll("[^0-9]", "");
        String phoneLike = digits.length() >= 4 ? "%" + digits + "%" : "§none§";

        List<SearchHit> users = em.createQuery("""
                select u.id, u.fullName, u.email, u.phone, u.role from User u
                 where u.id = :id
                    or lower(u.fullName) like :like
                    or lower(u.email) like :like
                    or u.phone like :phone
                 order by u.createdAt desc
                """, Object[].class)
                .setParameter("id", id)
                .setParameter("like", like)
                .setParameter("phone", phoneLike)
                .setMaxResults(LIMIT)
                .getResultList().stream()
                .map(x -> new SearchHit("USER", (UUID) x[0], (String) x[1],
                        x[3] == null ? (String) x[2] : x[2] + " · " + x[3], String.valueOf(x[4])))
                .toList();

        List<SearchHit> companies = em.createQuery("""
                select c.id, c.name, c.category, c.status, o.email from Company c
                  left join c.owner o
                 where c.id = :id
                    or lower(c.name) like :like
                    or lower(c.nameEn) like :like
                    or lower(c.nameRu) like :like
                    or lower(c.nameTr) like :like
                    or lower(o.email) like :like
                    or c.phone like :phone
                 order by c.createdAt desc
                """, Object[].class)
                .setParameter("id", id)
                .setParameter("like", like)
                .setParameter("phone", phoneLike)
                .setMaxResults(LIMIT)
                .getResultList().stream()
                .map(x -> new SearchHit("COMPANY", (UUID) x[0], (String) x[1],
                        x[4] == null ? String.valueOf(x[2]) : x[2] + " · " + x[4], String.valueOf(x[3])))
                .toList();
        return new SearchResult(query, users, companies);
    }

    private static UUID parseId(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return new UUID(0, 0);
        }
    }
}
