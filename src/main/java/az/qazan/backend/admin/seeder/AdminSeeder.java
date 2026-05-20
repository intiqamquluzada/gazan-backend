package az.qazan.backend.admin.seeder;

import az.qazan.backend.user.domain.AppLocale;
import az.qazan.backend.user.domain.Role;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Ensures a platform admin account exists so the admin panel is reachable.
 * Independent of {@code DevDataSeeder} — keys off the admin account being
 * absent, so it also provisions an already-seeded production database.
 */
@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class AdminSeeder {

    private static final String ADMIN_EMAIL = "admin@qazan.az";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner seedAdmin() {
        return args -> {
            if (users.findByEmailIgnoreCase(ADMIN_EMAIL).isPresent()) {
                log.info("Admin account already present — skipping admin seed.");
                return;
            }
            users.save(User.builder()
                    .email(ADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode("password123"))
                    .fullName("Qazan Admin")
                    .role(Role.ADMIN)
                    .locale(AppLocale.AZ)
                    .active(true)
                    .build());
            log.info("Admin account seeded: {}", ADMIN_EMAIL);
        };
    }
}
