package az.qazan.backend.admin.analytics;

import az.qazan.backend.admin.application.AdminAuditService;
import az.qazan.backend.common.api.ApiResponse;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Deep views behind the user and business pages of the web panel:
 * sign-in method, sessions, activity totals and a merged timeline.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminInsightController {

    private static final int TIMELINE = 60;

    private final EntityManager em;
    private final AdminAnalyticsService analytics;
    private final AdminAuditService audit;

    public record SessionInfo(UUID id, String channel, String ip, String userAgent,
                          Instant createdAt, Instant expiresAt) {}

    /** One line of the merged activity feed. {@code kind}: TAP, COIN, CLAIM, REVIEW, CARD. */
    public record InsightEvent(String kind, Instant at, String title, String detail, Long amount,
                        UUID companyId, String companyName, UUID userId, String userName,
                        boolean flagged) {}

    public record UserInsight(
            UUID id,
            String signInMethod,
            boolean authenticatorEnabled,
            long taps,
            long rejectedTaps,
            long coinsEarned,
            long coinsSpent,
            long rewardClaims,
            long rewardsUsed,
            long reviews,
            long favorites,
            long devices,
            Instant firstActivityAt,
            Instant lastActivityAt,
            List<SessionInfo> sessions,
            List<InsightEvent> timeline
    ) {}

    public record ProgramInfo(UUID id, String title, int stampsRequired, String rewardType,
                          boolean active, long cards, long rewardsClaimed) {}

    public record TagInfo(UUID id, String label, String kind, String status, int pointsValue,
                      int stampsValue, Instant provisionedAt, long taps, Instant lastTapAt) {}

    public record CompanyInsight(
            UUID id,
            long customers,
            long activeCustomers30d,
            long taps,
            long rejectedTaps,
            long suspiciousTaps,
            long coinsEarned,
            long coinsSpent,
            long rewardClaims,
            long reviews,
            long favorites,
            long staff,
            List<ProgramInfo> programs,
            List<TagInfo> tags,
            AnalyticsDtos.Series last90Days,
            List<InsightEvent> timeline
    ) {}

    // ───────────────────────────── users ─────────────────────────────

    @Operation(summary = "Everything about one user's activity and sessions")
    @GetMapping("/users/{id}/insight")
    @Transactional(readOnly = true)
    public UserInsight userInsight(@PathVariable UUID id) {
        Object[] u = single("select u.googleSub, u.appleSub, u.totpEnabledAt from User u where u.id = :id", id);
        if (u == null) throw NotFoundException.of(ErrorCode.USER_NOT_FOUND);
        String method = u[1] != null ? "APPLE" : u[0] != null ? "GOOGLE" : "EMAIL";

        List<SessionInfo> sessions = em.createQuery("""
                select t.id, t.channel, t.ip, t.userAgent, t.createdAt, t.expiresAt
                  from RefreshToken t
                 where t.user.id = :id and t.revokedAt is null and t.expiresAt > :now
                 order by t.createdAt desc
                """, Object[].class)
                .setParameter("id", id).setParameter("now", Instant.now())
                .setMaxResults(50).getResultList().stream()
                .map(x -> new SessionInfo((UUID) x[0], (String) x[1], (String) x[2], (String) x[3],
                        (Instant) x[4], (Instant) x[5]))
                .toList();

        List<InsightEvent> timeline = new ArrayList<>();
        em.createQuery("""
                select t.createdAt, t.result, t.pointsDelta, t.stampsDelta, t.companyId, c.name, t.suspicious, t.note
                  from NfcTap t left join Company c on c.id = t.companyId
                 where t.userId = :id order by t.createdAt desc
                """, Object[].class).setParameter("id", id).setMaxResults(TIMELINE)
                .getResultList().forEach(x -> timeline.add(tapEvent(x, id, null)));
        em.createQuery("""
                select t.createdAt, t.amount, t.note, c.id, c.name
                  from CoinTransaction t left join t.company c
                 where t.user.id = :id order by t.createdAt desc
                """, Object[].class).setParameter("id", id).setMaxResults(TIMELINE)
                .getResultList().forEach(x -> timeline.add(new InsightEvent("COIN", (Instant) x[0],
                        ((Number) x[1]).longValue() > 0 ? "EARN" : "SPEND", (String) x[2],
                        ((Number) x[1]).longValue(), (UUID) x[3], (String) x[4], id, null, false)));
        em.createQuery("""
                select r.createdAt, r.title, r.status, r.coinCost, r.companyId, c.name
                  from RewardClaim r left join Company c on c.id = r.companyId
                 where r.userId = :id order by r.createdAt desc
                """, Object[].class).setParameter("id", id).setMaxResults(TIMELINE)
                .getResultList().forEach(x -> timeline.add(new InsightEvent("CLAIM", (Instant) x[0],
                        (String) x[1], String.valueOf(x[2]), ((Number) x[3]).longValue(),
                        (UUID) x[4], (String) x[5], id, null, false)));
        em.createQuery("""
                select r.createdAt, r.rating, r.comment, r.companyId, c.name
                  from Review r left join Company c on c.id = r.companyId
                 where r.userId = :id order by r.createdAt desc
                """, Object[].class).setParameter("id", id).setMaxResults(TIMELINE)
                .getResultList().forEach(x -> timeline.add(new InsightEvent("REVIEW", (Instant) x[0],
                        x[1] + "★", (String) x[2], ((Number) x[1]).longValue(),
                        (UUID) x[3], (String) x[4], id, null, false)));
        timeline.sort(Comparator.comparing(InsightEvent::at).reversed());

        Object[] taps = single("""
                select sum(case when t.result = az.qazan.backend.nfc.domain.NfcTap.Result.OK then 1 else 0 end),
                       sum(case when t.result <> az.qazan.backend.nfc.domain.NfcTap.Result.OK then 1 else 0 end),
                       min(t.createdAt), max(t.createdAt)
                  from NfcTap t where t.userId = :id
                """, id);
        Object[] coins = single("""
                select sum(case when t.amount > 0 then t.amount else 0 end),
                       sum(case when t.amount < 0 then -t.amount else 0 end),
                       min(t.createdAt), max(t.createdAt)
                  from CoinTransaction t where t.user.id = :id
                """, id);
        Object[] claims = single("""
                select count(r), sum(case when r.status = az.qazan.backend.rewards.domain.RewardClaimStatus.USED then 1 else 0 end)
                  from RewardClaim r where r.userId = :id
                """, id);

        return new UserInsight(id, method, u[2] != null,
                num(taps, 0), num(taps, 1), num(coins, 0), num(coins, 1),
                num(claims, 0), num(claims, 1),
                count("select count(r) from Review r where r.userId = :id", id),
                countNative("select count(*) from favorites where user_id = :id", id),
                countNative("select count(*) from device_tokens where user_id = :id", id),
                earliest((Instant) at(taps, 2), (Instant) at(coins, 2)),
                latest((Instant) at(taps, 3), (Instant) at(coins, 3)),
                sessions,
                timeline.stream().limit(TIMELINE).toList());
    }

    @Operation(summary = "Sign a user out of every app and panel session")
    @PostMapping("/users/{id}/sessions/revoke")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> revokeSessions(@CurrentUser AppUserPrincipal me,
                                                            @PathVariable UUID id) {
        int n = em.createQuery("""
                update RefreshToken t set t.revokedAt = :now
                 where t.user.id = :id and t.revokedAt is null
                """).setParameter("now", Instant.now()).setParameter("id", id).executeUpdate();
        audit.record(me.getId(), "USER_SESSIONS_REVOKED", "USER", id, n + " sessions");
        return ResponseEntity.ok(ApiResponse.message("OK"));
    }

    // ─────────────────────────── businesses ───────────────────────────

    @Operation(summary = "Everything about one business: programs, tags, trend and feed")
    @GetMapping("/companies/{id}/insight")
    @Transactional(readOnly = true)
    public CompanyInsight companyInsight(@PathVariable UUID id) {
        if (single("select c.id, c.name from Company c where c.id = :id", id) == null) {
            throw NotFoundException.of(ErrorCode.NOT_FOUND);
        }
        Instant monthAgo = Instant.now().minusSeconds(30L * 86400);

        List<ProgramInfo> programs = em.createQuery("""
                select p.id, p.title, p.stampsRequired, p.rewardType, p.active,
                       (select count(c) from LoyaltyCard c where c.program.id = p.id),
                       (select coalesce(sum(c.totalRewardsClaimed), 0) from LoyaltyCard c where c.program.id = p.id)
                  from LoyaltyProgram p where p.company.id = :id order by p.createdAt
                """, Object[].class).setParameter("id", id).getResultList().stream()
                .map(x -> new ProgramInfo((UUID) x[0], (String) x[1], ((Number) x[2]).intValue(),
                        String.valueOf(x[3]), Boolean.TRUE.equals(x[4]),
                        ((Number) x[5]).longValue(), ((Number) x[6]).longValue()))
                .toList();

        List<TagInfo> tags = em.createQuery("""
                select t.id, t.label, t.kind, t.status, t.pointsValue, t.stampsValue, t.provisionedAt,
                       (select count(x) from NfcTap x where x.tagId = t.id),
                       (select max(x.createdAt) from NfcTap x where x.tagId = t.id)
                  from NfcTag t where t.company.id = :id order by t.createdAt
                """, Object[].class).setParameter("id", id).getResultList().stream()
                .map(x -> new TagInfo((UUID) x[0], (String) x[1], String.valueOf(x[2]), String.valueOf(x[3]),
                        ((Number) x[4]).intValue(), ((Number) x[5]).intValue(), (Instant) x[6],
                        ((Number) x[7]).longValue(), (Instant) x[8]))
                .toList();

        List<InsightEvent> timeline = new ArrayList<>();
        em.createQuery("""
                select t.createdAt, t.result, t.pointsDelta, t.stampsDelta, t.userId, u.fullName, t.suspicious, t.note
                  from NfcTap t left join User u on u.id = t.userId
                 where t.companyId = :id order by t.createdAt desc
                """, Object[].class).setParameter("id", id).setMaxResults(TIMELINE)
                .getResultList().forEach(x -> timeline.add(tapEvent(x, null, id)));
        em.createQuery("""
                select t.createdAt, t.amount, t.note, u.id, u.fullName
                  from CoinTransaction t join t.user u
                 where t.company.id = :id order by t.createdAt desc
                """, Object[].class).setParameter("id", id).setMaxResults(TIMELINE)
                .getResultList().forEach(x -> timeline.add(new InsightEvent("COIN", (Instant) x[0],
                        ((Number) x[1]).longValue() > 0 ? "EARN" : "SPEND", (String) x[2],
                        ((Number) x[1]).longValue(), id, null, (UUID) x[3], (String) x[4], false)));
        em.createQuery("""
                select r.createdAt, r.title, r.status, r.coinCost, r.userId, u.fullName
                  from RewardClaim r left join User u on u.id = r.userId
                 where r.companyId = :id order by r.createdAt desc
                """, Object[].class).setParameter("id", id).setMaxResults(TIMELINE)
                .getResultList().forEach(x -> timeline.add(new InsightEvent("CLAIM", (Instant) x[0],
                        (String) x[1], String.valueOf(x[2]), ((Number) x[3]).longValue(),
                        id, null, (UUID) x[4], (String) x[5], false)));
        em.createQuery("""
                select r.createdAt, r.rating, r.comment, r.userId, u.fullName
                  from Review r left join User u on u.id = r.userId
                 where r.companyId = :id order by r.createdAt desc
                """, Object[].class).setParameter("id", id).setMaxResults(TIMELINE)
                .getResultList().forEach(x -> timeline.add(new InsightEvent("REVIEW", (Instant) x[0],
                        x[1] + "★", (String) x[2], ((Number) x[1]).longValue(),
                        id, null, (UUID) x[3], (String) x[4], false)));
        timeline.sort(Comparator.comparing(InsightEvent::at).reversed());

        Object[] taps = single("""
                select sum(case when t.result = az.qazan.backend.nfc.domain.NfcTap.Result.OK then 1 else 0 end),
                       sum(case when t.result <> az.qazan.backend.nfc.domain.NfcTap.Result.OK then 1 else 0 end),
                       sum(case when t.suspicious = true then 1 else 0 end)
                  from NfcTap t where t.companyId = :id
                """, id);
        Object[] coins = single("""
                select sum(case when t.amount > 0 then t.amount else 0 end),
                       sum(case when t.amount < 0 then -t.amount else 0 end)
                  from CoinTransaction t where t.company.id = :id
                """, id);

        long active30 = em.createQuery("""
                select count(distinct t.userId) from NfcTap t
                 where t.companyId = :id and t.createdAt >= :since
                   and t.result = az.qazan.backend.nfc.domain.NfcTap.Result.OK
                """, Long.class).setParameter("id", id).setParameter("since", monthAgo).getSingleResult();

        LocalDate today = LocalDate.now(AdminAnalyticsService.ZONE);
        return new CompanyInsight(id,
                count("select count(distinct c.user.id) from LoyaltyCard c where c.company.id = :id", id),
                active30,
                num(taps, 0), num(taps, 1), num(taps, 2), num(coins, 0), num(coins, 1),
                count("select count(r) from RewardClaim r where r.companyId = :id", id),
                count("select count(r) from Review r where r.companyId = :id", id),
                countNative("select count(*) from favorites where company_id = :id", id),
                count("select count(u) from User u where u.staffCompanyId = :id", id),
                programs, tags,
                analytics.companySeries(id, today.minusDays(89), today),
                timeline.stream().limit(TIMELINE).toList());
    }

    // ───────────────────────────── helpers ─────────────────────────────

    private static InsightEvent tapEvent(Object[] x, UUID userId, UUID companyId) {
        long points = ((Number) x[2]).longValue();
        long stamps = ((Number) x[3]).longValue();
        String detail = points != 0 ? "+" + points + " coin" : stamps != 0 ? "+" + stamps + " stamp" : (String) x[7];
        return new InsightEvent("TAP", (Instant) x[0], String.valueOf(x[1]), detail,
                points != 0 ? points : stamps,
                companyId != null ? companyId : (UUID) x[4],
                companyId != null ? null : (String) x[5],
                userId != null ? userId : (UUID) x[4],
                userId != null ? null : (String) x[5],
                Boolean.TRUE.equals(x[6]));
    }

    private Object[] single(String jpql, UUID id) {
        List<Object[]> rows = em.createQuery(jpql, Object[].class).setParameter("id", id).getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    private long count(String jpql, UUID id) {
        Object v = em.createQuery(jpql).setParameter("id", id).getSingleResult();
        return v == null ? 0 : ((Number) v).longValue();
    }

    private long countNative(String sql, UUID id) {
        Object v = em.createNativeQuery(sql).setParameter("id", id).getSingleResult();
        return v == null ? 0 : ((Number) v).longValue();
    }

    private static long num(Object[] row, int i) {
        return row == null || row[i] == null ? 0 : ((Number) row[i]).longValue();
    }

    private static Object at(Object[] row, int i) {
        return row == null ? null : row[i];
    }

    private static Instant earliest(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    private static Instant latest(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
