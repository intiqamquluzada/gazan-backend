package az.qazan.backend.config;

import az.qazan.backend.common.exception.ApiError;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.i18n.MessageService;
import az.qazan.backend.common.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/email/start",
            "/api/v1/auth/email/verify",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/phone/start",
            "/api/v1/auth/social",
            "/api/v1/admin/auth/login",
            "/api/v1/admin/auth/verify",
            "/api/v1/admin/auth/refresh",
            "/api/v1/admin/auth/logout",
            "/legal/**",
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/docs",
            "/docs/**",
    };

    private final JwtAuthenticationFilter jwtFilter;
    private final ObjectMapper objectMapper;
    private final MessageService messages;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${app.cors.allowed-methods}")
    private String[] allowedMethods;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        cfg.setAllowedMethods(Arrays.asList(allowedMethods));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private AuthenticationEntryPoint authEntryPoint() {
        return (req, res, ex) -> writeError(res, ErrorCode.UNAUTHORIZED, req.getRequestURI());
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (req, res, ex) -> writeError(res, ErrorCode.FORBIDDEN, req.getRequestURI());
    }

    private void writeError(HttpServletResponse res, ErrorCode code, String path)
            throws java.io.IOException {
        res.setStatus(code.getStatus().value());
        // Without an explicit charset the servlet writer falls back to
        // ISO-8859-1, which mangles "ü", "ç", "ə" in the Azerbaijani and
        // Turkish messages. The app then cannot parse the 401, never
        // refreshes its token and shows "no internet" on every screen.
        res.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError err = new ApiError(
                code.name(),
                messages.get(code),
                code.getStatus().value(),
                path,
                Instant.now(),
                null,
                null
        );
        objectMapper.writeValue(res.getWriter(), err);
    }
}
