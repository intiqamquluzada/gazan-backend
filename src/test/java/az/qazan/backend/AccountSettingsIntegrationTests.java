package az.qazan.backend;

import az.qazan.backend.push.domain.DeviceToken;
import az.qazan.backend.push.domain.DevicePlatform;
import az.qazan.backend.push.domain.DeviceTokenRepository;
import az.qazan.backend.common.security.JwtTokenProvider;
import az.qazan.backend.user.domain.AppLocale;
import az.qazan.backend.user.domain.Role;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Password change (incl. Google / Apple accounts) and push switches. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:account;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.jwt.secret=test-secret-please-replace-with-256-bit-key-for-real-tests",
        "qazan.rate-limit.per-minute=1000",
})
class AccountSettingsIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired DeviceTokenRepository devices;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider jwt;

    private User user(boolean passwordSet) {
        return users.save(User.builder()
                .email(UUID.randomUUID() + "@test.az")
                .passwordHash(encoder.encode("password123"))
                .passwordSet(passwordSet)
                .fullName("Test")
                .role(Role.CUSTOMER)
                .locale(AppLocale.AZ)
                .active(true)
                .build());
    }

    private String bearer(User u) {
        return "Bearer " + jwt.issueAccessToken(u.getId(), u.getEmail(), u.getRole()).value();
    }

    @Test
    void changingPasswordKeepsThisDeviceSignedIn() throws Exception {
        User u = user(true);
        mvc.perform(post("/api/v1/users/me/change-password")
                        .header("Authorization", bearer(u))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"password123\",\"newPassword\":\"newpass456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.hasPassword").value(true));
        assertThat(encoder.matches("newpass456",
                users.findById(u.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void wrongCurrentPasswordIsABadRequest() throws Exception {
        User u = user(true);
        mvc.perform(post("/api/v1/users/me/change-password")
                        .header("Authorization", bearer(u))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"nope\",\"newPassword\":\"newpass456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_PASSWORD_MISMATCH"));
    }

    @Test
    void socialAccountSetsAPasswordWithoutACurrentOne() throws Exception {
        User u = user(false);
        mvc.perform(post("/api/v1/users/me/change-password")
                        .header("Authorization", bearer(u))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"firstpass1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.hasPassword").value(true));
        User after = users.findById(u.getId()).orElseThrow();
        assertThat(after.isPasswordSet()).isTrue();
        assertThat(encoder.matches("firstpass1", after.getPasswordHash())).isTrue();
    }

    @Test
    void switchedOffRewardsPushesAreNotSent() throws Exception {
        User on = user(true);
        User off = user(true);
        for (User u : List.of(on, off)) {
            DeviceToken t = new DeviceToken();
            t.setUserId(u.getId());
            t.setToken("tok-" + u.getId());
            t.setPlatform(DevicePlatform.ANDROID);
            devices.save(t);
        }
        mvc.perform(put("/api/v1/users/me/notification-prefs")
                        .header("Authorization", bearer(off))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rewards\":false,\"marketing\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationPrefs.rewards").value(false))
                .andExpect(jsonPath("$.notificationPrefs.offers").value(true));

        assertThat(devices.findTokensWantingRewards(List.of(on.getId(), off.getId())))
                .containsExactly("tok-" + on.getId());
    }
}
