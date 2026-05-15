package az.qazan.backend.common.audit;

import az.qazan.backend.common.security.AppUserPrincipal;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Tells Spring Data who the current actor is — used by {@code @CreatedBy}
 * and {@code @LastModifiedBy} when those columns are added later.
 */
@Component("auditorAware")
@Configuration
public class JpaAuditingConfig implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.of("system");
        if (auth.getPrincipal() instanceof AppUserPrincipal p) {
            return Optional.of(p.getId().toString());
        }
        return Optional.of(auth.getName());
    }
}
