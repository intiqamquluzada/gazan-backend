package az.qazan.backend.media.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Additive security chain that makes <strong>only</strong>
 * {@code GET /api/v1/images/**} public — so {@code <img>} / NetworkImage
 * (which carry no Authorization header) can render uploaded photos.
 *
 * <p>Higher precedence than the app's catch-all chain, and scoped to GET
 * only, so {@code POST /api/v1/images} still flows through the normal
 * authenticated chain (+ {@code @PreAuthorize}). Kept in its own module
 * config — the core {@code SecurityConfig} is left untouched.
 */
@Configuration
public class MediaSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain imagePublicChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(AntPathRequestMatcher.antMatcher(
                        HttpMethod.GET, "/api/v1/images/**"))
                .authorizeHttpRequests(reg -> reg.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults());
        return http.build();
    }
}
