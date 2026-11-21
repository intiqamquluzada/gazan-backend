package az.qazan.backend;

import az.qazan.backend.admin.application.AdminCoinService;
import az.qazan.backend.admin.application.AdminCompanyService;
import az.qazan.backend.admin.application.AdminStatsService;
import az.qazan.backend.admin.application.AdminUserService;
import az.qazan.backend.auth.application.PasswordResetService;
import az.qazan.backend.auth.domain.PasswordResetToken;
import az.qazan.backend.auth.domain.PasswordResetTokenRepository;
import az.qazan.backend.business.application.BusinessAnalyticsService;
import az.qazan.backend.business.application.BusinessCustomerDetailService;
import az.qazan.backend.business.application.BusinessCustomersService;
import az.qazan.backend.business.api.dto.BusinessCustomerResponse;
import az.qazan.backend.coins.application.CoinRewardService;
import az.qazan.backend.loyalty.api.dto.UpdateProgramRequest;
import az.qazan.backend.loyalty.application.LoyaltyCardService;
import az.qazan.backend.loyalty.application.LoyaltyProgramService;
import az.qazan.backend.loyalty.domain.LoyaltyCard;
import az.qazan.backend.loyalty.domain.LoyaltyProgram;
import az.qazan.backend.loyalty.domain.LoyaltyProgramRepository;
import az.qazan.backend.loyalty.domain.LoyaltyRewardType;
import az.qazan.backend.rewards.application.RewardTitles;
import org.springframework.context.i18n.LocaleContextHolder;
import java.math.BigDecimal;
import java.util.Locale;
import az.qazan.backend.business.application.BusinessStatsService;
import az.qazan.backend.coins.application.CoinService;
import az.qazan.backend.coins.domain.CoinReward;
import az.qazan.backend.coins.domain.CoinRewardRepository;
import az.qazan.backend.coins.domain.CoinTransactionRepository;
import az.qazan.backend.common.exception.AppException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.security.JwtTokenProvider;
import az.qazan.backend.companies.domain.BusinessCategory;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.companies.domain.CompanyStatus;
import az.qazan.backend.nfc.api.dto.NfcAssignRequest;
import az.qazan.backend.nfc.api.dto.NfcTagRequest;
import az.qazan.backend.nfc.api.dto.NfcTagResponse;
import az.qazan.backend.nfc.api.dto.NfcTapRequest;
import az.qazan.backend.nfc.api.dto.NfcTapResponse;
import az.qazan.backend.nfc.application.NfcService;
import az.qazan.backend.rewards.api.dto.SpendSessionResponse;
import az.qazan.backend.rewards.application.RewardService;
import az.qazan.backend.rewards.application.SpendSessionService;
import az.qazan.backend.rewards.domain.RewardClaimRepository;
import az.qazan.backend.rewards.domain.RewardClaimStatus;
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.jwt.secret=test-secret-please-replace-with-256-bit-key-for-real-tests",
        "qazan.rate-limit.per-minute=1000",
})
class AuthCoinsNfcIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CompanyRepository companies;
    @Autowired CoinRewardRepository coinRewards;
    @Autowired CoinTransactionRepository ledger;
    @Autowired RewardClaimRepository claims;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider jwt;
    @Autowired CoinService coins;
    @Autowired RewardService rewards;
    @Autowired PasswordResetService passwordReset;
    @Autowired NfcService nfc;
    @Autowired SpendSessionService spendSessions;
    @Autowired AdminStatsService adminStats;
    @Autowired AdminCompanyService adminCompanies;
    @Autowired AdminCoinService adminCoins;
    @Autowired AdminUserService adminUsers;
    @Autowired BusinessStatsService businessStats;
    @Autowired BusinessAnalyticsService analytics;
    @Autowired LoyaltyProgramRepository programRepo;
    @Autowired LoyaltyProgramService programService;
    @Autowired LoyaltyCardService loyaltyCards;
    @Autowired CoinRewardService coinRewardService;
    @Autowired BusinessCustomersService businessCustomers;
    @Autowired BusinessCustomerDetailService customerDetail;
    @Autowired RewardTitles rewardTitles;
    @Autowired az.qazan.backend.reviews.application.ReviewService reviewService;
    @Autowired az.qazan.backend.reviews.domain.ReviewRepository reviewRepo;
    @Autowired az.qazan.backend.reviews.domain.ReviewReportRepository reviewReports;

    private User user(Role role) {
        String key = UUID.randomUUID().toString();
        return users.save(User.builder()
                .email(key + "@test.az")
                .passwordHash(encoder.encode("password123"))
                .fullName("Test " + role)
                .role(role)
                .locale(AppLocale.AZ)
                .active(true)
                .build());
    }

    private Company company(User owner) {
        return companies.save(Company.builder()
                .name("Cafe " + owner.getId())
                .category(BusinessCategory.COFFEE)
                .coverColorHex(0xFF000000L)
                .reviewCount(0)
                .owner(owner)
                .status(CompanyStatus.APPROVED)
                .build());
    }

    private CoinReward reward(Company company, int cost) {
        return coinRewards.save(CoinReward.builder()
                .company(company)
                .title("Free coffee")
                .coinCost(cost)
                .active(true)
                .build());
    }

    private static String registerBody(String role, String phone) {
        String key = UUID.randomUUID().toString();
        String phoneField = phone == null ? "" : "\"phone\":\"" + phone + "\",";
        return "{\"email\":\"" + key + "@test.az\",\"password\":\"password123\","
                + "\"fullName\":\"Test User\"," + phoneField + "\"role\":\"" + role + "\"}";
    }

    private static NfcTapRequest tapRequest(String token, String uid) {
        return tapRequest(token, uid, 0);
    }

    private static NfcTapRequest tapRequest(String token, String uid, Integer ageSeconds) {
        return new NfcTapRequest(token, uid, null, null,
                UUID.randomUUID().toString(), "device", null, null, ageSeconds);
    }

    private LoyaltyProgram program(Company company, LoyaltyRewardType type,
                                   String value, String item, int stamps) {
        return programRepo.save(LoyaltyProgram.builder()
                .company(company)
                .title("Program " + type)
                .stampsRequired(stamps)
                .rewardType(type)
                .rewardValue(value == null ? null : new BigDecimal(value))
                .rewardItem(item)
                .active(true)
                .build());
    }

    private static UpdateProgramRequest programUpdate(Instant expiresAt, Boolean clear) {
        return new UpdateProgramRequest(null, null, null, null, null, null, null,
                expiresAt, clear);
    }

    private static void assertCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    void registerRejectsAdminRole() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("ADMIN", "+994501110000")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerNeedsPhoneAndPhonesAreUnique() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("CUSTOMER", null)))
                .andExpect(status().isBadRequest());

        String phone = "+99450" + (1000000 + (int) (Math.random() * 8999999));
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("CUSTOMER", phone)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.profileComplete").value(true));
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("CUSTOMER", phone)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_PHONE_TAKEN"));
        mvc.perform(post("/api/v1/auth/phone/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(true))
                .andExpect(jsonPath("$.verificationRequired").value(false));
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + phone
                                + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownAccountAndWrongPasswordLookTheSame() throws Exception {
        User u = user(Role.CUSTOMER);
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@test.az\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + u.getEmail() + "\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void socialLoginWithoutClientIdsIsUnavailable() throws Exception {
        mvc.perform(post("/api/v1/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"GOOGLE\",\"idToken\":\"abc\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AUTH_SOCIAL_NOT_CONFIGURED"));
    }

    @Test
    void blockedUserLosesAccessImmediately() throws Exception {
        User u = user(Role.CUSTOMER);
        String token = jwt.issueAccessToken(u.getId(), u.getEmail(), u.getRole()).value();
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        u.setActive(false);
        users.save(u);
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotRedeemWithoutCashier() throws Exception {
        User u = user(Role.CUSTOMER);
        String token = jwt.issueAccessToken(u.getId(), u.getEmail(), u.getRole()).value();
        mvc.perform(post("/api/v1/loyalty/cards/" + UUID.randomUUID() + "/redeem")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void grantRejectsAnyoneOutsideTheCompany() {
        User owner = user(Role.BUSINESS_OWNER);
        User stranger = user(Role.BUSINESS_OWNER);
        User customer = user(Role.CUSTOMER);
        Company cafe = company(owner);

        assertCode(() -> coins.grantByStaff(stranger.getId(), customer.getId(),
                cafe.getId(), 50, null), ErrorCode.FORBIDDEN);
        assertCode(() -> coins.grantByStaff(owner.getId(), customer.getId(),
                cafe.getId(), 10_001, null), ErrorCode.GRANT_CAP_EXCEEDED);

        coins.grantByStaff(owner.getId(), customer.getId(), cafe.getId(), 50, null);
        assertThat(ledger.balanceOfAtCompany(customer.getId(), cafe.getId())).isEqualTo(50);
    }

    @Test
    void spendOnlyCountsThatCompanysCoins() {
        User customer = user(Role.CUSTOMER);
        Company a = company(user(Role.BUSINESS_OWNER));
        Company b = company(user(Role.BUSINESS_OWNER));
        coins.grant(customer.getId(), a.getId(), 100, null);

        assertCode(() -> coins.spend(customer.getId(), b.getId(), 10, null),
                ErrorCode.COIN_INSUFFICIENT);
        coins.spend(customer.getId(), a.getId(), 100, null);
        assertThat(ledger.balanceOf(customer.getId())).isZero();
    }

    @Test
    void resetCodeLocksAfterFiveWrongTries() {
        User u = user(Role.CUSTOMER);
        resetTokens.save(PasswordResetToken.builder()
                .userId(u.getId())
                .codeHash(encoder.encode("123456"))
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .build());

        for (int i = 0; i < 5; i++) {
            assertCode(() -> passwordReset.confirm(u.getEmail(), "000000", "newpassword1"),
                    ErrorCode.AUTH_RESET_CODE_INVALID);
        }
        // Even the right code no longer works.
        assertCode(() -> passwordReset.confirm(u.getEmail(), "123456", "newpassword1"),
                ErrorCode.AUTH_RESET_LOCKED);
    }

    @Test
    void parallelPurchasesCannotOverdraw() throws Exception {
        User customer = user(Role.CUSTOMER);
        Company cafe = company(user(Role.BUSINESS_OWNER));
        CoinReward gift = reward(cafe, 100);
        coins.grant(customer.getId(), cafe.getId(), 100, null);

        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<Boolean>> results = new ArrayList<>();
        Callable<Boolean> buy = () -> {
            try {
                rewards.purchase(customer.getId(), gift.getId());
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        };
        for (int i = 0; i < 4; i++) {
            results.add(pool.submit(buy));
        }
        int bought = 0;
        for (Future<Boolean> r : results) {
            if (r.get()) bought++;
        }
        pool.shutdown();

        assertThat(bought).isEqualTo(1);
        assertThat(ledger.balanceOfAtCompany(customer.getId(), cafe.getId())).isZero();
        assertThat(claims.findAllByUserIdAndStatusOrderByCreatedAtDesc(
                customer.getId(), RewardClaimStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void pointsCardThenPurchaseThenSpendCardReachesTheCashier() {
        User owner = user(Role.BUSINESS_OWNER);
        User customer = user(Role.CUSTOMER);
        Company cafe = company(owner);
        CoinReward gift = reward(cafe, 100);

        NfcTagResponse pointsCard = nfc.issue(new NfcTagRequest(
                cafe.getId(), null, null, "points", null, null, 0, null));
        // Nothing happens until the owner assigns it.
        assertCode(() -> nfc.tap(customer.getId(), tapRequest(pointsCard.token(), "AA01")),
                ErrorCode.NFC_TAG_UNASSIGNED);
        assertCode(() -> nfc.tap(customer.getId(), tapRequest(pointsCard.token(), null)),
                ErrorCode.NFC_MISSING_UID);
        // A removed card kind is refused.
        assertCode(() -> nfc.assign(pointsCard.id(), new NfcAssignRequest(
                "GIFT", null, null, null, null, null, null)), ErrorCode.BAD_REQUEST);
        nfc.assign(pointsCard.id(), new NfcAssignRequest(
                "POINTS", null, 100, null, null, null, null));

        NfcTapResponse earned = nfc.tap(customer.getId(), tapRequest(pointsCard.token(), "AA01"));
        assertThat(earned.kind()).isEqualTo("POINTS");
        assertThat(earned.pointsAdded()).isEqualTo(100);
        assertCode(() -> nfc.tap(customer.getId(), tapRequest(pointsCard.token(), "BB02")),
                ErrorCode.NFC_UNKNOWN_TAG);
        rewards.purchase(customer.getId(), gift.getId());

        NfcTagResponse spendCard = nfc.issue(new NfcTagRequest(
                cafe.getId(), "SPEND", null, "till", null, null, null, null));
        NfcTapResponse opened = nfc.tap(customer.getId(), tapRequest(spendCard.token(), "CC03"));
        assertThat(opened.spendSessionId()).isNotNull();

        List<SpendSessionResponse> screen = spendSessions.open(owner.getId());
        assertThat(screen).hasSize(1);
        SpendSessionResponse.Reward item = screen.get(0).rewards().get(0);

        SpendSessionResponse after = spendSessions.use(
                owner.getId(), opened.spendSessionId(), item.kind(), item.id());
        assertThat(after.rewards()).isEmpty();
        assertThat(spendSessions.forCustomer(customer.getId(), opened.spendSessionId())
                .usedTitles()).containsExactly("Free coffee");

        // No rewards left, so the card has nothing to open.
        spendSessions.close(owner.getId(), opened.spendSessionId());
        assertCode(() -> nfc.tap(customer.getId(), tapRequest(spendCard.token(), "CC03")),
                ErrorCode.NFC_NO_REWARDS);
    }

    @Test
    void suspendedVenueCannotTakeTaps() {
        User owner = user(Role.BUSINESS_OWNER);
        User admin = user(Role.ADMIN);
        Company cafe = company(owner);
        NfcTagResponse card = nfc.issue(new NfcTagRequest(
                cafe.getId(), "POINTS", null, null, 100, null, null, null));

        adminCompanies.setStatus(admin.getId(), cafe.getId(), CompanyStatus.SUSPENDED, "test");

        assertCode(() -> nfc.tap(user(Role.CUSTOMER).getId(),
                tapRequest(card.token(), "DD04")), ErrorCode.COMPANY_NOT_APPROVED);
    }

    @Test
    void reportingQueriesRun() {
        User owner = user(Role.BUSINESS_OWNER);
        User admin = user(Role.ADMIN);
        User customer = user(Role.CUSTOMER);
        Company cafe = company(owner);
        coins.grant(customer.getId(), cafe.getId(), 40, null);
        adminCoins.adjust(admin.getId(), customer.getId(), cafe.getId(), -5, "fix");

        assertThat(adminStats.overview().signups()).hasSize(14);
        assertThat(adminCoins.byCompany(30)).isNotEmpty();
        assertThat(adminUsers.detail(customer.getId()).coinBalance()).isEqualTo(35);
        assertThat(adminCompanies.detail(cafe.getId()).coinsSpent()).isEqualTo(5);
        assertThat(businessStats.statsForOwner(owner.getId()).activeCustomers()).isEqualTo(1);
        assertThat(analytics.forOwner(owner.getId(), 30).daily()).hasSize(30);
        assertThat(analytics.forOwner(owner.getId(), 500).daily()).hasSize(90);
    }

    // ─────────────────── campaign-flow fixes ───────────────────

    @Test
    void rewardTitlesFollowTheRewardType() {
        User owner = user(Role.BUSINESS_OWNER);
        Company cafe = company(owner);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        try {
            assertThat(rewardTitles.forProgram(
                    program(cafe, LoyaltyRewardType.FREE_ITEM, null, "coffee", 5)))
                    .isEqualTo("Free coffee");
            assertThat(rewardTitles.forProgram(
                    program(cafe, LoyaltyRewardType.PERCENTAGE_DISCOUNT, "10.00", "coffee", 5)))
                    .isEqualTo("10% off coffee");
            assertThat(rewardTitles.forProgram(
                    program(cafe, LoyaltyRewardType.FIXED_DISCOUNT, "5", null, 5)))
                    .isEqualTo("5 ₼ off");
            assertThat(rewardTitles.forProgram(
                    program(cafe, LoyaltyRewardType.CASHBACK, "2.50", null, 5)))
                    .isEqualTo("2.5 ₼ cashback");
            assertThat(rewardTitles.forProgram(
                    program(cafe, LoyaltyRewardType.FIXED_DISCOUNT, "5", null, 5),
                    Locale.of("az"))).isEqualTo("5 ₼ endirim");

            // A completed stamp card names its gift in the tap response and in
            // the cashier's pending list, with the machine-readable type.
            User customer = user(Role.CUSTOMER);
            LoyaltyProgram percent = program(cafe,
                    LoyaltyRewardType.PERCENTAGE_DISCOUNT, "10", "coffee", 1);
            NfcTagResponse card = nfc.issue(new NfcTagRequest(
                    cafe.getId(), "STAMP", percent.getId(), null, null, 1, 0, null));
            NfcTapResponse tap = nfc.tap(customer.getId(), tapRequest(card.token(), "AB01"));
            assertThat(tap.rewardUnlocked()).isTrue();
            assertThat(tap.unlockedRewardTitle()).isEqualTo("10% off coffee");
            var pending = rewards.pendingAtMyCompany(owner.getId());
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).title()).isEqualTo("10% off coffee");
            assertThat(pending.get(0).rewardType()).isEqualTo("PERCENTAGE_DISCOUNT");
            assertThat(pending.get(0).currency()).isEqualTo("AZN");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void tapShowsTheBalanceAtThatVenueOnly() {
        User ownerA = user(Role.BUSINESS_OWNER);
        User ownerB = user(Role.BUSINESS_OWNER);
        User customer = user(Role.CUSTOMER);
        Company cafeA = company(ownerA);
        Company cafeB = company(ownerB);
        coins.grant(customer.getId(), cafeB.getId(), 500, "elsewhere");

        NfcTagResponse card = nfc.issue(new NfcTagRequest(
                cafeA.getId(), "POINTS", null, null, 100, null, 0, null));
        NfcTapResponse tap = nfc.tap(customer.getId(), tapRequest(card.token(), "AC01"));

        assertThat(tap.pointsAdded()).isEqualTo(100);
        assertThat(tap.pointsBalance()).isEqualTo(100);
    }

    @Test
    void programWithPendingGiftsCannotBeDeleted() {
        User owner = user(Role.BUSINESS_OWNER);
        User customer = user(Role.CUSTOMER);
        Company cafe = company(owner);
        LoyaltyProgram p = program(cafe, LoyaltyRewardType.FREE_ITEM, null, "tea", 1);
        LoyaltyCard card = loyaltyCards.joinProgram(customer.getId(), p.getId());
        loyaltyCards.addStamp(card.getId(), 1, "test");

        assertCode(() -> programService.delete(p.getId(), owner.getId()),
                ErrorCode.PROGRAM_HAS_PENDING_REWARDS);
    }

    @Test
    void closedProgramCannotBeAssignedOrJoined() {
        User owner = user(Role.BUSINESS_OWNER);
        Company cafe = company(owner);
        LoyaltyProgram p = program(cafe, LoyaltyRewardType.FREE_ITEM, null, "tea", 5);
        p.setActive(false);
        programRepo.save(p);
        NfcTagResponse card = nfc.issue(new NfcTagRequest(
                cafe.getId(), null, null, null, null, null, null, null));

        assertCode(() -> nfc.assign(card.id(), new NfcAssignRequest(
                "STAMP", p.getId(), null, 1, null, null, null)), ErrorCode.PROGRAM_CLOSED);
        assertCode(() -> loyaltyCards.joinProgram(user(Role.CUSTOMER).getId(), p.getId()),
                ErrorCode.PROGRAM_CLOSED);
    }

    @Test
    void endDateCanBeCleared() {
        User owner = user(Role.BUSINESS_OWNER);
        Company cafe = company(owner);
        LoyaltyProgram p = program(cafe, LoyaltyRewardType.FREE_ITEM, null, "tea", 5);
        Instant end = Instant.now().plus(Duration.ofDays(30));

        programService.update(p.getId(), owner.getId(), programUpdate(end, null));
        assertThat(programRepo.findById(p.getId()).orElseThrow().getExpiresAt()).isNotNull();

        // Nothing sent: the date stays.
        programService.update(p.getId(), owner.getId(), programUpdate(null, null));
        assertThat(programRepo.findById(p.getId()).orElseThrow().getExpiresAt()).isNotNull();

        programService.update(p.getId(), owner.getId(), programUpdate(null, true));
        assertThat(programRepo.findById(p.getId()).orElseThrow().getExpiresAt()).isNull();
    }

    @Test
    void ownerCanEditAndPauseCatalogItems() {
        User owner = user(Role.BUSINESS_OWNER);
        User customer = user(Role.CUSTOMER);
        Company cafe = company(owner);
        CoinReward item = reward(cafe, 100);

        coinRewardService.update(item.getId(), owner.getId(), "Big coffee", null, 150, false);

        assertThat(coinRewardService.listForCaller(cafe.getId(), false, owner.getId(), false))
                .extracting(CoinReward::getTitle).containsExactly("Big coffee");
        assertThat(coinRewardService.listForCaller(cafe.getId(), false, customer.getId(), false))
                .isEmpty();
        assertThat(coinRewardService.listForCaller(cafe.getId(), true, owner.getId(), false))
                .isEmpty();
        coins.grant(customer.getId(), cafe.getId(), 500, "test");
        assertCode(() -> rewards.purchase(customer.getId(), item.getId()),
                ErrorCode.REWARD_INACTIVE);
        assertCode(() -> coinRewardService.update(item.getId(), customer.getId(),
                null, null, null, true), ErrorCode.FORBIDDEN);
    }

    @Test
    void lateSpendTapFromTheOfflineQueueOpensNoWindow() {
        User owner = user(Role.BUSINESS_OWNER);
        User customer = user(Role.CUSTOMER);
        Company cafe = company(owner);
        CoinReward item = reward(cafe, 100);
        coins.grant(customer.getId(), cafe.getId(), 100, "test");
        rewards.purchase(customer.getId(), item.getId());
        NfcTagResponse spend = nfc.issue(new NfcTagRequest(
                cafe.getId(), "SPEND", null, null, null, null, null, null));

        assertCode(() -> nfc.tap(customer.getId(), tapRequest(spend.token(), "AD01", 300)),
                ErrorCode.NFC_TAP_TOO_OLD);
        assertThat(spendSessions.open(owner.getId())).isEmpty();

        NfcTapResponse live = nfc.tap(customer.getId(), tapRequest(spend.token(), "AD01", 5));
        assertThat(live.spendSessionId()).isNotNull();
        assertThat(spendSessions.open(owner.getId())).hasSize(1);
    }

    @Test
    void coinOnlyCustomersAreListedWithTheirVenueBalance() {
        User owner = user(Role.BUSINESS_OWNER);
        User cashier = user(Role.BUSINESS_OWNER);
        User customer = user(Role.CUSTOMER);
        Company cafe = company(owner);
        company(cashier);
        coins.grant(customer.getId(), cafe.getId(), 250, "test");

        List<BusinessCustomerResponse> list = businessCustomers.customersForOwner(owner.getId());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).userId()).isEqualTo(customer.getId());
        assertThat(list.get(0).cardCount()).isZero();
        assertThat(list.get(0).coinBalance()).isEqualTo(250);
        assertThat(customerDetail.detail(owner.getId(), customer.getId()).coinBalance())
                .isEqualTo(250);
    }

    @Test
    void aUsedVoucherCannotBeUsedAgain() {
        User owner = user(Role.BUSINESS_OWNER);
        User customer = user(Role.CUSTOMER);
        Company cafe = company(owner);
        CoinReward item = reward(cafe, 100);
        coins.grant(customer.getId(), cafe.getId(), 100, "test");
        UUID voucher = rewards.purchase(customer.getId(), item.getId()).id();

        rewards.use(owner.getId(), "COIN", voucher, customer.getId());
        assertCode(() -> rewards.use(owner.getId(), "COIN", voucher, customer.getId()),
                ErrorCode.VOUCHER_ALREADY_USED);
    }

    @Test
    void reviewsCanBeReportedOnceButNotByTheirAuthor() throws Exception {
        User owner = user(Role.BUSINESS_OWNER);
        Company cafe = company(owner);
        User author = user(Role.CUSTOMER);
        User reader = user(Role.CUSTOMER);
        var review = reviewRepo.save(az.qazan.backend.reviews.domain.Review.builder()
                .userId(author.getId()).companyId(cafe.getId()).rating(1).comment("bad").build());

        reviewService.report(reader.getId(), review.getId(),
                az.qazan.backend.reviews.domain.ReviewReportReason.ABUSIVE, "rude");
        // A repeat is a quiet no-op.
        reviewService.report(reader.getId(), review.getId(),
                az.qazan.backend.reviews.domain.ReviewReportReason.SPAM, null);
        assertThat(reviewReports.countByReviewId(review.getId())).isEqualTo(1);

        assertCode(() -> reviewService.report(author.getId(), review.getId(),
                az.qazan.backend.reviews.domain.ReviewReportReason.OTHER, null), ErrorCode.REVIEW_REPORT_OWN);

        var reported = reviewRepo.findReportedForAdmin(org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(reported).anySatisfy(v -> {
            assertThat(v.getId()).isEqualTo(review.getId());
            assertThat(v.getReportCount()).isEqualTo(1);
        });
        assertThat(reviewRepo.findLatestForAdmin(org.springframework.data.domain.PageRequest.of(0, 50)))
                .anySatisfy(v -> assertThat(v.getReportCount()).isEqualTo(1));
    }

    @Test
    void legalPagesArePublicAndMarkMissingDetails() throws Exception {
        mvc.perform(get("/legal/privacy").param("lang", "tr"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("KVKK")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("TAMAMLANACAK")));
        mvc.perform(get("/legal/terms")).andExpect(status().isOk());
        mvc.perform(get("/legal/delete-account").header("Accept-Language", "ru-RU,ru;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Удаление аккаунта")));
    }
}
