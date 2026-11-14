package az.qazan.backend.admin.seeder;

import az.qazan.backend.admin.domain.AdminUserRepository;
import az.qazan.backend.user.domain.AppLocale;
import az.qazan.backend.user.domain.Role;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin in any environment, from ADMIN_EMAIL and
 * ADMIN_PASSWORD. Does nothing once an admin exists.
 */
@Slf4j
@Component
public class AdminBootstrap implements ApplicationRunner {

    private final AdminUserRepository admins;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(AdminUserRepository admins,
                          UserRepository users,
                          PasswordEncoder passwordEncoder,
                          @Value("${ADMIN_EMAIL:}") String email,
                          @Value("${ADMIN_PASSWORD:}") String password) {
        this.admins = admins;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()
                || admins.countByRole(Role.ADMIN) > 0
                || users.existsByEmailIgnoreCase(email)) {
            return;
        }
        users.save(User.builder()
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .fullName("Qazan Admin")
                .role(Role.ADMIN)
                .locale(AppLocale.AZ)
                .active(true)
                .build());
        log.info("First admin account created from ADMIN_EMAIL.");
    }
}
