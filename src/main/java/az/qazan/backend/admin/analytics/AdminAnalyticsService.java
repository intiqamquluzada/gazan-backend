package az.qazan.backend.admin.analytics;

import az.qazan.backend.admin.analytics.AnalyticsDtos.Breakdown;
import az.qazan.backend.admin.analytics.AnalyticsDtos.Bucket;
import az.qazan.backend.admin.analytics.AnalyticsDtos.Kpi;
import az.qazan.backend.admin.analytics.AnalyticsDtos.Overview;
import az.qazan.backend.admin.analytics.AnalyticsDtos.Point;
import az.qazan.backend.admin.analytics.AnalyticsDtos.Series;
import az.qazan.backend.admin.analytics.AnalyticsDtos.Slice;
import az.qazan.backend.admin.analytics.AnalyticsDtos.TopCompany;
import az.qazan.backend.admin.analytics.AnalyticsDtos.TopCustomer;
import az.qazan.backend.admin.analytics.AnalyticsDtos.Totals;
import az.qazan.backend.common.exception.BadRequestException;
import az.qazan.backend.common.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Platform analytics for the web admin panel. Periods are whole days on the
 * platform's home clock (Asia/Baku), {@code from} and {@code to} inclusive.
 *
 * <p>Rows are read as narrow tuples and folded in memory, which keeps the
 * queries portable and is plenty at today's volume. Move the folds into SQL
 * (date_trunc + group by) once a period holds millions of rows.
 */
@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    public static final ZoneId ZONE = ZoneId.of("Asia/Baku");
    private static final long MAX_DAYS = 731;
    private static final int TOP = 10;

    private final EntityManager em;

    // ───────────────────────────── overview ─────────────────────────────

    @Transactional(readOnly = true)
    public Overview overview(LocalDate from, LocalDate to) {
        Range cur = Range.of(from, to);
        Range prev = cur.previous();
        Map<String, Double> a = figures(cur);
        Map<String, Double> b = figures(prev);
        Map<String, Kpi> kpis = new LinkedHashMap<>();
        a.forEach((k, v) -> kpis.put(k, new Kpi(v, b.getOrDefault(k, 0d))));
        return new Overview(cur.from, cur.to, prev.from, prev.to, kpis, totals());
    }

    private Map<String, Double> figures(Range r) {
        Data d = load(r);
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("newUsers", (double) d.users.size());
        m.put("newCustomers", (double) d.users.stream().filter(u -> "CUSTOMER".equals(u.role)).count());
        m.put("newBusinessOwners", (double) d.users.stream().filter(u -> "BUSINESS_OWNER".equals(u.role)).count());
        m.put("newCompanies", (double) d.companies.size());
        m.put("activeUsers", (double) d.activeUsers().size());
        m.put("activeCompanies", (double) d.activeCompanies().size());
        m.put("coinsEarned", (double) d.coins.stream().filter(c -> c.amount > 0).mapToLong(c -> c.amount).sum());
        m.put("coinsSpent", (double) -d.coins.stream().filter(c -> c.amount < 0).mapToLong(c -> c.amount).sum());
        m.put("coinTransactions", (double) d.coins.size());
        m.put("taps", (double) d.taps.stream().filter(t -> t.ok).count());
        m.put("rejectedTaps", (double) d.taps.stream().filter(t -> !t.ok).count());
        m.put("suspiciousTaps", (double) d.taps.stream().filter(t -> t.suspicious).count());
        m.put("rewardClaims", (double) d.claims.size());
        m.put("rewardsUsed", (double) d.claims.stream().filter(c -> c.used).count());
        m.put("newCards", (double) d.cards.size());
        m.put("reviews", (double) d.reviews.size());
        m.put("avgRating", d.reviews.isEmpty() ? 0d
                : Math.round(d.reviews.stream().mapToInt(x -> x.rating).average().orElse(0) * 100) / 100d);
        return m;
    }

    private Totals totals() {
        return new Totals(
                count("select count(u) from User u"),
                count("select count(u) from User u where u.role = az.qazan.backend.user.domain.Role.CUSTOMER"),
                count("select count(u) from User u where u.role = az.qazan.backend.user.domain.Role.BUSINESS_OWNER"),
                count("select count(c) from Company c"),
                count("select count(c) from Company c where c.status = az.qazan.backend.companies.domain.CompanyStatus.APPROVED"),
                count("select count(c) from Company c where c.status = az.qazan.backend.companies.domain.CompanyStatus.PENDING"),
                count("select count(c) from LoyaltyCard c"),
                count("select coalesce(sum(t.amount), 0) from CoinTransaction t"),
                count("select count(t) from NfcTag t"));
    }

    // ──────────────────────────── time series ────────────────────────────

    @Transactional(readOnly = true)
    public Series timeseries(LocalDate from, LocalDate to, Bucket bucket) {
        Range r = Range.of(from, to);
        return series(r, bucket == null ? Bucket.DAY : bucket, load(r));
    }

    /** Daily activity of one business (no sign-up figures). */
    @Transactional(readOnly = true)
    public Series companySeries(UUID companyId, LocalDate from, LocalDate to) {
        Range r = Range.of(from, to);
        return series(r, Bucket.DAY, loadCompany(r, companyId));
    }

    private Series series(Range r, Bucket b, Data d) {

        TreeMap<LocalDate, long[]> acc = new TreeMap<>();
        Map<LocalDate, Set<UUID>> active = new HashMap<>();
        for (LocalDate day = bucketStart(r.from, b); !day.isAfter(r.to); day = next(day, b)) {
            acc.put(day, new long[10]);
            active.put(day, new HashSet<>());
        }
        Function<Instant, LocalDate> key = t -> bucketStart(t.atZone(ZONE).toLocalDate(), b);

        d.users.forEach(u -> bump(acc, key.apply(u.at), 0, 1));
        d.companies.forEach(c -> bump(acc, key.apply(c.at), 1, 1));
        d.coins.forEach(c -> {
            LocalDate k = key.apply(c.at);
            if (c.amount > 0) bump(acc, k, 2, c.amount); else bump(acc, k, 3, -c.amount);
            bump(acc, k, 4, 1);
            if (c.userId != null && active.containsKey(k)) active.get(k).add(c.userId);
        });
        d.taps.forEach(t -> {
            LocalDate k = key.apply(t.at);
            bump(acc, k, t.ok ? 5 : 6, 1);
            if (t.ok && t.userId != null && active.containsKey(k)) active.get(k).add(t.userId);
        });
        d.claims.forEach(c -> {
            LocalDate k = key.apply(c.at);
            bump(acc, k, 7, 1);
            if (c.userId != null && active.containsKey(k)) active.get(k).add(c.userId);
        });
        d.cards.forEach(c -> bump(acc, key.apply(c), 8, 1));
        d.reviews.forEach(x -> bump(acc, key.apply(x.at), 9, 1));

        List<Point> points = acc.entrySet().stream().map(e -> {
            long[] v = e.getValue();
            return new Point(e.getKey(), v[0], v[1], active.get(e.getKey()).size(),
                    v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9]);
        }).toList();
        return new Series(r.from, r.to, b, points);
    }

    private static void bump(Map<LocalDate, long[]> acc, LocalDate k, int i, long by) {
        long[] v = acc.get(k);
        if (v != null) v[i] += by;
    }

    private static LocalDate bucketStart(LocalDate d, Bucket b) {
        return switch (b) {
            case DAY -> d;
            case WEEK -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> d.withDayOfMonth(1);
        };
    }

    private static LocalDate next(LocalDate d, Bucket b) {
        return switch (b) {
            case DAY -> d.plusDays(1);
            case WEEK -> d.plusWeeks(1);
            case MONTH -> d.plusMonths(1);
        };
    }

    // ───────────────────────────── breakdown ─────────────────────────────

    @Transactional(readOnly = true)
    public Breakdown breakdown(LocalDate from, LocalDate to) {
        Range r = Range.of(from, to);
        Data d = load(r);

        List<Slice> methods = slices(d.users.stream().map(u -> u.method).toList());
        List<Slice> roles = slices(d.users.stream().map(u -> u.role).toList());
        List<Slice> tapResults = slices(d.taps.stream().map(t -> t.result).toList());
        List<Slice> ratings = slices(d.reviews.stream().map(x -> x.rating + "").toList());

        List<Object[]> companyRows = em.createQuery(
                "select c.id, c.name, c.category, c.status from Company c", Object[].class)
                .getResultList();
        List<Slice> byCategory = slices(companyRows.stream().map(x -> String.valueOf(x[2])).toList());
        List<Slice> byStatus = slices(companyRows.stream().map(x -> String.valueOf(x[3])).toList());

        // Top businesses by activity in the period.
        Map<UUID, long[]> perCompany = new HashMap<>();
        Map<UUID, Set<UUID>> companyCustomers = new HashMap<>();
        d.taps.stream().filter(t -> t.ok && t.companyId != null).forEach(t -> {
            perCompany.computeIfAbsent(t.companyId, k -> new long[4])[0]++;
            if (t.userId != null) companyCustomers.computeIfAbsent(t.companyId, k -> new HashSet<>()).add(t.userId);
        });
        d.coins.stream().filter(c -> c.companyId != null).forEach(c -> {
            long[] v = perCompany.computeIfAbsent(c.companyId, k -> new long[4]);
            if (c.amount > 0) v[1] += c.amount; else v[2] -= c.amount;
            if (c.userId != null) companyCustomers.computeIfAbsent(c.companyId, k -> new HashSet<>()).add(c.userId);
        });
        d.claims.stream().filter(c -> c.companyId != null)
                .forEach(c -> perCompany.computeIfAbsent(c.companyId, k -> new long[4])[3]++);
        Map<UUID, Object[]> companyById = companyRows.stream()
                .collect(Collectors.toMap(x -> (UUID) x[0], x -> x));
        List<TopCompany> topCompanies = perCompany.entrySet().stream()
                .filter(e -> companyById.containsKey(e.getKey()))
                .sorted(Comparator.comparingLong((Map.Entry<UUID, long[]> e) ->
                        e.getValue()[0] * 10 + e.getValue()[1] + e.getValue()[2] + e.getValue()[3] * 10).reversed())
                .limit(TOP)
                .map(e -> {
                    Object[] c = companyById.get(e.getKey());
                    long[] v = e.getValue();
                    return new TopCompany(e.getKey(), (String) c[1], String.valueOf(c[2]),
                            v[0], v[1], v[2], v[3],
                            companyCustomers.getOrDefault(e.getKey(), Set.of()).size());
                }).toList();

        // Top customers.
        Map<UUID, long[]> perUser = new HashMap<>();
        d.taps.stream().filter(t -> t.ok && t.userId != null)
                .forEach(t -> perUser.computeIfAbsent(t.userId, k -> new long[4])[0]++);
        d.coins.stream().filter(c -> c.userId != null).forEach(c -> {
            long[] v = perUser.computeIfAbsent(c.userId, k -> new long[4]);
            if (c.amount > 0) v[1] += c.amount; else v[2] -= c.amount;
        });
        d.claims.stream().filter(c -> c.userId != null)
                .forEach(c -> perUser.computeIfAbsent(c.userId, k -> new long[4])[3]++);
        List<Map.Entry<UUID, long[]>> topUserEntries = perUser.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<UUID, long[]> e) ->
                        e.getValue()[0] * 10 + e.getValue()[1] + e.getValue()[3] * 10).reversed())
                .limit(TOP).toList();
        Map<UUID, Object[]> userById = topUserEntries.isEmpty() ? Map.of()
                : em.createQuery("select u.id, u.fullName, u.email from User u where u.id in :ids", Object[].class)
                    .setParameter("ids", topUserEntries.stream().map(Map.Entry::getKey).toList())
                    .getResultList().stream().collect(Collectors.toMap(x -> (UUID) x[0], x -> x));
        List<TopCustomer> topCustomers = topUserEntries.stream()
                .filter(e -> userById.containsKey(e.getKey()))
                .map(e -> {
                    Object[] u = userById.get(e.getKey());
                    long[] v = e.getValue();
                    return new TopCustomer(e.getKey(), (String) u[1], (String) u[2], v[0], v[1], v[2], v[3]);
                }).toList();

        long[][] heat = new long[7][24];
        d.taps.stream().filter(t -> t.ok).forEach(t -> heat(heat, t.at));
        d.coins.forEach(c -> heat(heat, c.at));

        return new Breakdown(r.from, r.to, methods, roles, byCategory, byStatus,
                tapResults, ratings, topCompanies, topCustomers, heat);
    }

    private static void heat(long[][] heat, Instant at) {
        ZonedDateTime z = at.atZone(ZONE);
        heat[z.getDayOfWeek().getValue() - 1][z.getHour()]++;
    }

    private static List<Slice> slices(List<String> keys) {
        return keys.stream()
                .collect(Collectors.groupingBy(k -> k, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new Slice(e.getKey(), e.getValue()))
                .toList();
    }

    // ───────────────────────────── loading ─────────────────────────────

    private record UserRow(Instant at, String role, String method) {}
    private record CompanyRow(Instant at) {}
    private record CoinRow(Instant at, long amount, UUID userId, UUID companyId) {}
    private record TapRow(Instant at, boolean ok, String result, boolean suspicious, UUID userId, UUID companyId) {}
    private record ClaimRow(Instant at, boolean used, UUID userId, UUID companyId) {}
    private record ReviewRow(Instant at, int rating) {}

    private record Data(List<UserRow> users, List<CompanyRow> companies, List<CoinRow> coins,
                        List<TapRow> taps, List<ClaimRow> claims, List<Instant> cards,
                        List<ReviewRow> reviews) {

        Set<UUID> activeUsers() {
            Set<UUID> s = new HashSet<>();
            coins.forEach(c -> { if (c.userId != null) s.add(c.userId); });
            taps.forEach(t -> { if (t.ok && t.userId != null) s.add(t.userId); });
            claims.forEach(c -> { if (c.userId != null) s.add(c.userId); });
            return s;
        }

        Set<UUID> activeCompanies() {
            Set<UUID> s = new HashSet<>();
            coins.forEach(c -> { if (c.companyId != null) s.add(c.companyId); });
            taps.forEach(t -> { if (t.ok && t.companyId != null) s.add(t.companyId); });
            claims.forEach(c -> { if (c.companyId != null) s.add(c.companyId); });
            return s;
        }
    }

    private Data load(Range r) {
        List<UserRow> users = rows("select u.createdAt, u.role, u.googleSub, u.appleSub from User u"
                + " where u.createdAt >= :a and u.createdAt < :b", r).stream()
                .map(x -> new UserRow((Instant) x[0], String.valueOf(x[1]),
                        x[3] != null ? "APPLE" : x[2] != null ? "GOOGLE" : "EMAIL"))
                .toList();
        List<CompanyRow> companies = rows("select c.createdAt, c.id from Company c"
                + " where c.createdAt >= :a and c.createdAt < :b", r).stream()
                .map(x -> new CompanyRow((Instant) x[0])).toList();
        List<CoinRow> coins = rows("select t.createdAt, t.amount, t.user.id, t.company.id"
                + " from CoinTransaction t where t.createdAt >= :a and t.createdAt < :b", r).stream()
                .map(x -> new CoinRow((Instant) x[0], ((Number) x[1]).longValue(), (UUID) x[2], (UUID) x[3]))
                .toList();
        List<TapRow> taps = rows("select t.createdAt, t.result, t.suspicious, t.userId, t.companyId"
                + " from NfcTap t where t.createdAt >= :a and t.createdAt < :b", r).stream()
                .map(x -> new TapRow((Instant) x[0], "OK".equals(String.valueOf(x[1])),
                        String.valueOf(x[1]), Boolean.TRUE.equals(x[2]), (UUID) x[3], (UUID) x[4]))
                .toList();
        List<ClaimRow> claims = rows("select c.createdAt, c.status, c.userId, c.companyId"
                + " from RewardClaim c where c.createdAt >= :a and c.createdAt < :b", r).stream()
                .map(x -> new ClaimRow((Instant) x[0], "USED".equals(String.valueOf(x[1])),
                        (UUID) x[2], (UUID) x[3]))
                .toList();
        List<Instant> cards = rows("select c.createdAt, c.id from LoyaltyCard c"
                + " where c.createdAt >= :a and c.createdAt < :b", r).stream()
                .map(x -> (Instant) x[0]).toList();
        List<ReviewRow> reviews = rows("select x.createdAt, x.rating from Review x"
                + " where x.createdAt >= :a and x.createdAt < :b", r).stream()
                .map(x -> new ReviewRow((Instant) x[0], ((Number) x[1]).intValue())).toList();
        return new Data(users, companies, coins, taps, claims, cards, reviews);
    }

    private Data loadCompany(Range r, UUID companyId) {
        List<CoinRow> coins = rows("select t.createdAt, t.amount, t.user.id, t.company.id"
                + " from CoinTransaction t where t.createdAt >= :a and t.createdAt < :b"
                + " and t.company.id = :c", r, companyId).stream()
                .map(x -> new CoinRow((Instant) x[0], ((Number) x[1]).longValue(), (UUID) x[2], (UUID) x[3]))
                .toList();
        List<TapRow> taps = rows("select t.createdAt, t.result, t.suspicious, t.userId, t.companyId"
                + " from NfcTap t where t.createdAt >= :a and t.createdAt < :b"
                + " and t.companyId = :c", r, companyId).stream()
                .map(x -> new TapRow((Instant) x[0], "OK".equals(String.valueOf(x[1])),
                        String.valueOf(x[1]), Boolean.TRUE.equals(x[2]), (UUID) x[3], (UUID) x[4]))
                .toList();
        List<ClaimRow> claims = rows("select c.createdAt, c.status, c.userId, c.companyId"
                + " from RewardClaim c where c.createdAt >= :a and c.createdAt < :b"
                + " and c.companyId = :c", r, companyId).stream()
                .map(x -> new ClaimRow((Instant) x[0], "USED".equals(String.valueOf(x[1])),
                        (UUID) x[2], (UUID) x[3]))
                .toList();
        List<Instant> cards = rows("select c.createdAt, c.id from LoyaltyCard c"
                + " where c.createdAt >= :a and c.createdAt < :b and c.company.id = :c", r, companyId)
                .stream().map(x -> (Instant) x[0]).toList();
        List<ReviewRow> reviews = rows("select x.createdAt, x.rating from Review x"
                + " where x.createdAt >= :a and x.createdAt < :b and x.companyId = :c", r, companyId)
                .stream().map(x -> new ReviewRow((Instant) x[0], ((Number) x[1]).intValue())).toList();
        return new Data(List.of(), List.of(), coins, taps, claims, cards, reviews);
    }

    private List<Object[]> rows(String jpql, Range r) {
        return em.createQuery(jpql, Object[].class)
                .setParameter("a", r.start())
                .setParameter("b", r.end())
                .getResultList();
    }

    private List<Object[]> rows(String jpql, Range r, UUID companyId) {
        return em.createQuery(jpql, Object[].class)
                .setParameter("a", r.start())
                .setParameter("b", r.end())
                .setParameter("c", companyId)
                .getResultList();
    }

    private long count(String jpql) {
        Object v = em.createQuery(jpql).getSingleResult();
        return v == null ? 0 : ((Number) v).longValue();
    }

    /** Whole days, both ends included. */
    record Range(LocalDate from, LocalDate to) {
        static Range of(LocalDate from, LocalDate to) {
            LocalDate t = to == null ? LocalDate.now(ZONE) : to;
            LocalDate f = from == null ? t.minusDays(29) : from;
            if (f.isAfter(t) || ChronoUnit.DAYS.between(f, t) > MAX_DAYS) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST);
            }
            return new Range(f, t);
        }

        Range previous() {
            long days = ChronoUnit.DAYS.between(from, to) + 1;
            return new Range(from.minusDays(days), from.minusDays(1));
        }

        Instant start() { return from.atStartOfDay(ZONE).toInstant(); }

        Instant end() { return to.plusDays(1).atStartOfDay(ZONE).toInstant(); }
    }

}
