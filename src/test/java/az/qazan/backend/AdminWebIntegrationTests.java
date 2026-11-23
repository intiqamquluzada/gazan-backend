package az.qazan.backend;

import az.qazan.backend.admin.auth.AdminAuthController;
import az.qazan.backend.admin.auth.TotpService;
import az.qazan.backend.coins.application.CoinService;
import az.qazan.backend.companies.domain.BusinessCategory;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.companies.domain.CompanyStatus;
import az.qazan.backend.user.domain.AppLocale;
import az.qazan.backend.user.domain.Role;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:adminweb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.jwt.secret=test-secret-please-replace-with-256-bit-key-for-real-tests",
        "qazan.rate-limit.per-minute=1000",
        "qazan.admin.require-mfa=true",
})
class AdminWebIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CompanyRepository companies;
    @Autowired PasswordEncoder encoder;
    @Autowired TotpService totp;
    @Autowired CoinService coins;
    @Autowired ObjectMapper json;

    private User user(Role role) {
        return users.save(User.builder()
                .email(UUID.randomUUID() + "@test.az")
                .passwordHash(encoder.encode("password123"))
                .fullName("Test " + role)
                .role(role)
                .locale(AppLocale.AZ)
                .active(true)
                .build());
    }

    private JsonNode login(User u, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + u.getEmail() + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private MvcResult verify(String challenge, String code) throws Exception {
        return mvc.perform(post("/api/v1/admin/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeToken\":\"" + challenge + "\",\"code\":\"" + code + "\"}"))
                .andReturn();
    }

    /** Signs in end to end; returns the access token and the session cookie. */
    private Object[] signIn(User admin) throws Exception {
        JsonNode start = login(admin, "password123");
        String secret = start.path("setup").path("secret").asText(null);
        if (secret == null) {
            secret = totp.decrypt(users.findById(admin.getId()).orElseThrow().getTotpSecret());
        }
        MvcResult r = verify(start.get("challengeToken").asText(), totp.currentCode(secret));
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return new Object[]{
                json.readTree(r.getResponse().getContentAsString()).get("accessToken").asText(),
                r.getResponse().getCookie(AdminAuthController.COOKIE)};
    }

    @Test
    void firstSignInEnrolsTheAuthenticatorThenAsksForCodes() throws Exception {
        User admin = user(Role.ADMIN);

        JsonNode first = login(admin, "password123");
        assertThat(first.get("enrolled").asBoolean()).isFalse();
        String secret = first.get("setup").get("secret").asText();
        assertThat(first.get("setup").get("otpauthUrl").asText()).startsWith("otpauth://totp/");

        // A wrong code is refused and doesn't enrol.
        assertThat(verify(first.get("challengeToken").asText(), "000000").getResponse().getStatus())
                .isEqualTo(401);

        MvcResult ok = verify(first.get("challengeToken").asText(), totp.currentCode(secret));
        assertThat(ok.getResponse().getStatus()).isEqualTo(200);
        Cookie cookie = ok.getResponse().getCookie(AdminAuthController.COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/admin/auth");

        // Same code twice is a replay.
        JsonNode second = login(admin, "password123");
        assertThat(second.get("enrolled").asBoolean()).isTrue();
        assertThat(second.has("setup")).isFalse();
        assertThat(verify(second.get("challengeToken").asText(), totp.currentCode(secret))
                .getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void nonAdminsAndWrongPasswordsGetTheSameAnswer() throws Exception {
        User owner = user(Role.BUSINESS_OWNER);
        mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + owner.getEmail() + "\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
        User admin = user(Role.ADMIN);
        mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + admin.getEmail() + "\",\"password\":\"nope-nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void challengeIsNotAnAccessTokenAndAppTokensNeedMfaWhenRequired() throws Exception {
        User admin = user(Role.ADMIN);
        String challenge = login(admin, "password123").get("challengeToken").asText();
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + challenge))
                .andExpect(status().isUnauthorized());

        // A plain app sign-in (no authenticator) is refused on admin routes.
        MvcResult app = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + admin.getEmail() + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn();
        String appToken = json.readTree(app.getResponse().getContentAsString()).get("accessToken").asText();
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + appToken))
                .andExpect(status().isUnauthorized());

        String panelToken = (String) signIn(admin)[0];
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + panelToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/auth/me").header("Authorization", "Bearer " + panelToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(admin.getEmail()));
    }

    @Test
    void cookieRefreshRotatesAndLogoutEndsTheSession() throws Exception {
        User admin = user(Role.ADMIN);
        Cookie cookie = (Cookie) signIn(admin)[1];

        // Without the marker header the cookie alone does nothing.
        mvc.perform(post("/api/v1/admin/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());

        MvcResult r = mvc.perform(post("/api/v1/admin/auth/refresh").cookie(cookie)
                        .header(AdminAuthController.CSRF_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.idleTimeoutSeconds").value(1800))
                .andReturn();
        Cookie rotated = r.getResponse().getCookie(AdminAuthController.COOKIE);
        assertThat(rotated.getValue()).isNotEqualTo(cookie.getValue());

        // The old one is spent, and the panel cookie never works on the app route.
        mvc.perform(post("/api/v1/admin/auth/refresh").cookie(cookie)
                        .header(AdminAuthController.CSRF_HEADER, "1"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotated.getValue() + "\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/admin/auth/logout").cookie(rotated)
                        .header(AdminAuthController.CSRF_HEADER, "1"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/auth/refresh").cookie(rotated)
                        .header(AdminAuthController.CSRF_HEADER, "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void analyticsSearchAndInsightsAnswer() throws Exception {
        User admin = user(Role.ADMIN);
        String token = (String) signIn(admin)[0];
        User owner = user(Role.BUSINESS_OWNER);
        Company c = companies.save(Company.builder()
                .name("Analytics Cafe " + UUID.randomUUID())
                .category(BusinessCategory.COFFEE)
                .coverColorHex(0xFF000000L)
                .reviewCount(0)
                .owner(owner)
                .status(CompanyStatus.APPROVED)
                .build());
        User customer = user(Role.CUSTOMER);
        coins.grant(customer.getId(), c.getId(), 120, "test");

        mvc.perform(get("/api/v1/admin/analytics/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.coinsEarned.value").value(org.hamcrest.Matchers.greaterThanOrEqualTo(120.0)))
                .andExpect(jsonPath("$.totals.users").isNumber());
        mvc.perform(get("/api/v1/admin/analytics/timeseries?bucket=WEEK").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").isArray());
        mvc.perform(get("/api/v1/admin/analytics/breakdown").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityHeatmap.length()").value(7))
                .andExpect(jsonPath("$.topCompanies[0].id").isNotEmpty());
        mvc.perform(get("/api/v1/admin/analytics/overview?from=2026-02-01&to=2026-01-01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/admin/search").param("q", "Analytics Cafe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies[0].type").value("COMPANY"));

        mvc.perform(get("/api/v1/admin/users/" + customer.getId() + "/insight")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coinsEarned").value(120))
                .andExpect(jsonPath("$.signInMethod").value("EMAIL"))
                .andExpect(jsonPath("$.timeline[0].kind").value("COIN"));
        mvc.perform(get("/api/v1/admin/companies/" + c.getId() + "/insight")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coinsEarned").value(120))
                .andExpect(jsonPath("$.last90Days.points.length()").value(90));
        mvc.perform(post("/api/v1/admin/users/" + customer.getId() + "/sessions/revoke")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
