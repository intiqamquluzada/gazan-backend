package az.qazan.backend.nfc.application;

import az.qazan.backend.coins.application.CoinService;
import az.qazan.backend.common.exception.AppException;
import az.qazan.backend.common.exception.BadRequestException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.companies.domain.CompanyStatus;
import az.qazan.backend.loyalty.application.LoyaltyCardService;
import az.qazan.backend.loyalty.domain.LoyaltyCard;
import az.qazan.backend.loyalty.domain.LoyaltyProgram;
import az.qazan.backend.loyalty.domain.LoyaltyProgramRepository;
import az.qazan.backend.nfc.api.dto.NfcAssignRequest;
import az.qazan.backend.nfc.api.dto.NfcTagLimitsRequest;
import az.qazan.backend.nfc.api.dto.NfcTagRequest;
import az.qazan.backend.nfc.api.dto.NfcTagResponse;
import az.qazan.backend.nfc.api.dto.NfcTapRequest;
import az.qazan.backend.nfc.api.dto.NfcTapResponse;
import az.qazan.backend.nfc.domain.NfcTag;
import az.qazan.backend.nfc.domain.NfcTagRepository;
import az.qazan.backend.nfc.domain.NfcTap;
import az.qazan.backend.nfc.domain.NfcTapRepository;
import az.qazan.backend.rewards.application.RewardTitles;
import az.qazan.backend.rewards.application.SpendSessionService;
import az.qazan.backend.rewards.domain.SpendSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * NFC card issuing + the tap pipeline.
 *
 * <p>The cashier taps a venue card on the customer's phone; the phone reads
 * a token and posts it here. With static NTAG 213 tags the token can be
 * copied by anyone who reads the card, so acceptance is guarded by:
 * card status, UID binding (learned on first tap), per-user cooldown,
 * per-user daily cap, a geofence around the venue and a full audit trail.
 * Every rejected attempt is still written to {@code nfc_taps} so the admin
 * panel can spot abuse. When NTAG 424 DNA cards arrive, the counter/CMAC
 * fields make replay impossible without touching this flow.
 */
@Service
@RequiredArgsConstructor
public class NfcService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<NfcTag.Status> OWNER_STATUSES = Set.of(
            NfcTag.Status.ACTIVE, NfcTag.Status.SUSPENDED, NfcTag.Status.LOST);

    /** A refused tap: carries what the audit row needs once the lock is gone. */
    private static final class TapRejected extends AppException {
        private final transient NfcTag tag;
        private final transient Company company;
        private final NfcTap.Result result;
        private final Integer distance;
        private final boolean suspicious;
        private final String note;

        TapRejected(ErrorCode code, NfcTag tag, Company company, NfcTap.Result result,
                    Integer distance, boolean suspicious, String note) {
            super(code);
            this.tag = tag;
            this.company = company;
            this.result = result;
            this.distance = distance;
            this.suspicious = suspicious;
            this.note = note;
        }
    }

    /** A "spend" tap older than this (offline queue) no longer opens a window. */
    private static final int MAX_SPEND_AGE_SECONDS = 120;

    private final NfcAuditService audit;
    private final NfcTagRepository tags;
    private final NfcTapRepository taps;
    private final CompanyRepository companies;
    private final LoyaltyProgramRepository programs;
    private final LoyaltyCardService cards;
    private final CoinService coins;
    private final SpendSessionService spendSessions;
    private final TransactionTemplate transactions;
    private final RewardTitles rewardTitles;

    // ───────────────────────── tap ─────────────────────────

    /**
     * Runs the tap in its own transaction and writes the audit row for a
     * refusal only after that transaction has ended. The audit insert has
     * a foreign key to the tag, so doing it while the tag is still locked
     * would make it wait on itself.
     */
    public NfcTapResponse tap(UUID userId, NfcTapRequest req) {
        try {
            return transactions.execute(status -> processTap(userId, req));
        } catch (TapRejected r) {
            audit.recordRejected(
                    r.tag == null ? null : r.tag.getId(),
                    userId,
                    r.company == null ? null : r.company.getId(),
                    r.result,
                    r.distance,
                    req.deviceId(),
                    req.clientTapId(),
                    r.suspicious,
                    r.note);
            throw r;
        }
    }

    private NfcTapResponse processTap(UUID userId, NfcTapRequest req) {
        NfcTag tag = tags.lockByToken(req.token().trim())
                .flatMap(t -> tags.findByIdWithRefs(t.getId()))
                .orElse(null);
        if (tag == null) {
            throw new TapRejected(ErrorCode.NFC_UNKNOWN_TAG, null, null,
                    NfcTap.Result.UNKNOWN_TAG, null, true, "token not found");
        }

        // Retry of a tap we already processed (offline queue) → no double
        // credit. Checked under the card lock so parallel retries line up.
        if (req.clientTapId() != null && !req.clientTapId().isBlank()) {
            Optional<NfcTap> prior =
                    taps.findByUserIdAndClientTapId(userId, req.clientTapId());
            // Only an accepted tap short-circuits; a rejected one may legitimately
            // be retried (e.g. the customer moved closer to the venue).
            if (prior.isPresent() && prior.get().getResult() == NfcTap.Result.OK) {
                return replay(prior.get(), tag);
            }
        }

        Company company = tag.getCompany();

        if (!tag.isUsable()) {
            throw new TapRejected(ErrorCode.NFC_TAG_REVOKED, tag, company,
                    NfcTap.Result.REVOKED, null, true, null);
        }
        if (company.getStatus() != CompanyStatus.APPROVED) {
            throw new TapRejected(ErrorCode.COMPANY_NOT_APPROVED, tag, company,
                    NfcTap.Result.NOT_APPROVED, null, false, null);
        }
        if ("QR".equalsIgnoreCase(company.getLoyaltyMode())) {
            throw new TapRejected(ErrorCode.NFC_MODE_OFF, tag, company,
                    NfcTap.Result.MODE_OFF, null, false, null);
        }

        // Chip serial: required on every read, learned on the first one and
        // enforced afterwards. A copy of the token on another chip fails.
        String uid = req.tagUid() == null ? "" : req.tagUid().trim().toUpperCase();
        if (uid.isEmpty()) {
            throw new TapRejected(ErrorCode.NFC_MISSING_UID, tag, company,
                    NfcTap.Result.MISSING_UID, null, true, null);
        }
        if (tag.getTagUid() == null || tag.getTagUid().isBlank()) {
            tag.setTagUid(uid);
        } else if (!tag.getTagUid().equalsIgnoreCase(uid)) {
            throw new TapRejected(ErrorCode.NFC_UNKNOWN_TAG, tag, company,
                    NfcTap.Result.WRONG_UID, null, true, "uid " + uid);
        }

        // Dynamic tags (NTAG 424 DNA): the read counter must move forward.
        long counter = req.counter() == null ? 0L : req.counter();
        if (counter > 0 && counter <= tag.getLastCounter()) {
            throw new TapRejected(ErrorCode.NFC_REPLAY, tag, company,
                    NfcTap.Result.REPLAY, null, true, null);
        }

        requireTarget(tag, company);

        // Geofence: the phone should be at the venue.
        Integer distance = distanceMeters(company, req.latitude(), req.longitude());
        boolean suspicious = distance == null; // no location → accept, but flag
        if (distance != null && company.getNfcGeofenceM() > 0
                && distance > company.getNfcGeofenceM()) {
            throw new TapRejected(ErrorCode.NFC_OUT_OF_RANGE, tag, company,
                    NfcTap.Result.OUT_OF_RANGE, distance, true, null);
        }

        if (tag.getKind() == NfcTag.Kind.SPEND
                && req.ageSeconds() != null && req.ageSeconds() > MAX_SPEND_AGE_SECONDS) {
            throw new TapRejected(ErrorCode.NFC_TAP_TOO_OLD, tag, company,
                    NfcTap.Result.TOO_OLD, distance, false,
                    "queued " + req.ageSeconds() + "s");
        }
        if (tag.getKind() == NfcTag.Kind.SPEND) {
            return openSpendSession(userId, req, tag, company, counter, distance, suspicious);
        }

        enforceLimits(userId, tag, company, req, distance);

        // ── accepted ──
        if (counter > 0) {
            tag.setLastCounter(counter);
        }
        int pointsAdded = 0;
        int stampsAdded = 0;
        Integer stampsNow = null;
        Integer stampsRequired = null;
        String programTitle = null;
        boolean rewardUnlocked = false;
        String unlockedRewardTitle = null;

        switch (tag.getKind()) {
            case STAMP -> {
                LoyaltyProgram program = tag.getProgram();
                LoyaltyCard card = cards.joinProgram(userId, program.getId());
                int before = card.getRewardsAvailable();
                card = cards.addStamp(card.getId(), tag.getStampsValue(), "NFC");
                stampsAdded = tag.getStampsValue();
                stampsNow = card.getStamps();
                stampsRequired = card.getStampsRequired();
                rewardUnlocked = card.getRewardsAvailable() > before;
                programTitle = program.getTitle();
                if (rewardUnlocked) {
                    unlockedRewardTitle = rewardTitles.forProgram(program);
                }
            }
            case POINTS -> {
                coins.grant(userId, company.getId(), tag.getPointsValue(), "NFC");
                pointsAdded = tag.getPointsValue();
            }
            default -> throw new IllegalStateException("Unhandled card kind");
        }

        accept(tag, userId, company, req, pointsAdded, stampsAdded, distance, suspicious);

        return new NfcTapResponse(
                tag.getKind().name(),
                company.getId(),
                company.getName(),
                programTitle,
                stampsAdded,
                stampsNow,
                stampsRequired,
                pointsAdded,
                coins.balanceAt(userId, company.getId()),
                rewardUnlocked,
                null,
                unlockedRewardTitle);
    }

    /** An unassigned card, or one whose campaign is closed, does nothing. */
    private void requireTarget(NfcTag tag, Company company) {
        boolean ready = switch (tag.getKind()) {
            case UNASSIGNED -> false;
            case STAMP -> tag.getProgram() != null && tag.getProgram().isOpen();
            case POINTS, SPEND -> true;
        };
        if (!ready) {
            throw new TapRejected(ErrorCode.NFC_TAG_UNASSIGNED, tag, company,
                    NfcTap.Result.UNASSIGNED, null, false, null);
        }
    }

    private void enforceLimits(UUID userId, NfcTag tag, Company company,
                               NfcTapRequest req, Integer distance) {
        // Cooldown between two accepted taps of the same card by the same user.
        if (tag.getCooldownSec() > 0) {
            List<NfcTap> last = taps.lastWithResult(
                    userId, tag.getId(), NfcTap.Result.OK, PageRequest.of(0, 1));
            if (!last.isEmpty()) {
                long elapsed = Duration.between(last.get(0).getCreatedAt(), Instant.now())
                        .getSeconds();
                if (elapsed < tag.getCooldownSec()) {
                    throw new TapRejected(ErrorCode.NFC_COOLDOWN, tag, company,
                            NfcTap.Result.COOLDOWN, distance, false,
                            "retry in " + (tag.getCooldownSec() - elapsed) + "s");
                }
            }
        }

        // Daily cap per user per card, on the venue's own calendar. Without
        // coordinates the geofence could not run, so only one tap a day counts.
        ZoneId zone = company.venueZone();
        Instant dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        long today = taps.countWithResultSince(
                userId, tag.getId(), NfcTap.Result.OK, dayStart);
        boolean venueHasLocation = company.getLatitude() != null && company.getLongitude() != null;
        boolean phoneSentLocation = req.latitude() != null && req.longitude() != null;
        int cap = venueHasLocation && !phoneSentLocation
                ? Math.min(1, tag.getDailyCapPerUser())
                : tag.getDailyCapPerUser();
        if (today >= cap) {
            throw new TapRejected(ErrorCode.NFC_DAILY_CAP, tag, company,
                    NfcTap.Result.DAILY_CAP, distance, false, null);
        }
    }

    /** SPEND cards credit nothing: they put the customer on the cashier's screen. */
    private NfcTapResponse openSpendSession(UUID userId, NfcTapRequest req, NfcTag tag,
                                            Company company, long counter,
                                            Integer distance, boolean suspicious) {
        if (!spendSessions.hasUsableRewards(userId, company.getId())) {
            throw new TapRejected(ErrorCode.NFC_NO_REWARDS, tag, company,
                    NfcTap.Result.NO_REWARDS, distance, false, null);
        }
        if (counter > 0) {
            tag.setLastCounter(counter);
        }
        SpendSession session = spendSessions.openOrRefresh(
                company.getId(), userId, tag.getId());
        accept(tag, userId, company, req, 0, 0, distance, suspicious);
        return new NfcTapResponse(
                NfcTag.Kind.SPEND.name(), company.getId(), company.getName(),
                null, 0, null, null, 0, coins.balanceAt(userId, company.getId()),
                false, session.getId(), null);
    }

    /** Re-answer an already processed tap without crediting again. */
    private NfcTapResponse replay(NfcTap prior, NfcTag tag) {
        Company company = tag.getCompany();
        boolean spend = tag.getKind() == NfcTag.Kind.SPEND;
        return new NfcTapResponse(
                tag.getKind().name(),
                company.getId(),
                company.getName(),
                tag.getProgram() == null ? null : tag.getProgram().getTitle(),
                prior.getStampsDelta(),
                null,
                null,
                prior.getPointsDelta(),
                coins.balanceAt(prior.getUserId(), company.getId()),
                false,
                spend ? spendSessions.openSessionId(company.getId(), prior.getUserId()) : null,
                null);
    }

    /** Accepted tap — written inside the same transaction as the credit. */
    private void accept(NfcTag tag, UUID userId, Company company, NfcTapRequest req,
                        int points, int stamps, Integer distance, boolean suspicious) {
        taps.save(NfcTap.builder()
                .tagId(tag.getId())
                .userId(userId)
                .companyId(company.getId())
                .counter(req.counter() == null ? 0L : req.counter())
                .result(NfcTap.Result.OK)
                .pointsDelta(points)
                .stampsDelta(stamps)
                .distanceM(distance)
                .deviceId(req.deviceId())
                .clientTapId(req.clientTapId())
                .suspicious(suspicious)
                .build());
    }

    /** Great-circle distance in metres, or null when either side has no coordinates. */
    private Integer distanceMeters(Company c, Double lat, Double lng) {
        if (lat == null || lng == null || c.getLatitude() == null || c.getLongitude() == null) {
            return null;
        }
        double earth = 6371000;
        double dLat = Math.toRadians(lat - c.getLatitude());
        double dLng = Math.toRadians(lng - c.getLongitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(c.getLatitude())) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return (int) Math.round(earth * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    // ───────────────────────── card management ─────────────────────────

    @Transactional
    public NfcTagResponse issue(NfcTagRequest req) {
        Company company = companies.findById(req.companyId())
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        NfcTag.Kind kind = req.kind() == null || req.kind().isBlank()
                ? NfcTag.Kind.UNASSIGNED
                : parseKind(req.kind());
        return NfcTagResponse.of(tags.save(NfcTag.builder()
                .company(company)
                .program(kind == NfcTag.Kind.STAMP ? programOf(company, req.programId()) : null)
                .kind(kind)
                .label(req.label())
                .token(newToken())
                .keyVersion(1)
                .lastCounter(0)
                .pointsValue(req.pointsValue() == null ? 100 : req.pointsValue())
                .stampsValue(req.stampsValue() == null ? 1 : req.stampsValue())
                .cooldownSec(req.cooldownSec() == null ? 300 : req.cooldownSec())
                .dailyCapPerUser(req.dailyCapPerUser() == null ? 3 : req.dailyCapPerUser())
                .status(NfcTag.Status.ACTIVE)
                .build()), true);
    }

    /** New secret for the same card — used after a card is lost or copied. */
    @Transactional
    public NfcTagResponse rotateToken(UUID tagId) {
        NfcTag tag = get(tagId);
        tag.setToken(newToken());
        tag.setTagUid(null);
        tag.setProvisionedAt(null);
        tag.setStatus(NfcTag.Status.ACTIVE);
        return NfcTagResponse.of(tag, true);
    }

    @Transactional
    public NfcTagResponse setStatus(UUID tagId, String status) {
        NfcTag tag = get(tagId);
        applyStatus(tag, parseStatus(status));
        return NfcTagResponse.of(tag, false);
    }

    /** Owners can pause or report a card, but only an admin can revive a revoked one. */
    @Transactional
    public NfcTagResponse setStatusByOwner(UUID tagId, String status) {
        NfcTag tag = get(tagId);
        NfcTag.Status next = parseStatus(status);
        if (!OWNER_STATUSES.contains(next) || tag.getStatus() == NfcTag.Status.REVOKED) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        applyStatus(tag, next);
        return NfcTagResponse.of(tag, false);
    }

    private static void applyStatus(NfcTag tag, NfcTag.Status status) {
        tag.setStatus(status);
        tag.setRevokedAt(status == NfcTag.Status.ACTIVE ? null : Instant.now());
    }

    @Transactional
    public NfcTagResponse markProvisioned(UUID tagId) {
        NfcTag tag = get(tagId);
        tag.setProvisionedAt(Instant.now());
        return NfcTagResponse.of(tag, false);
    }

    @Transactional
    public NfcTagResponse updateLimits(UUID tagId, NfcTagLimitsRequest req) {
        NfcTag tag = get(tagId);
        if (req.label() != null) tag.setLabel(req.label());
        if (req.pointsValue() != null) tag.setPointsValue(req.pointsValue());
        if (req.stampsValue() != null) tag.setStampsValue(req.stampsValue());
        if (req.cooldownSec() != null) tag.setCooldownSec(req.cooldownSec());
        if (req.dailyCapPerUser() != null) tag.setDailyCapPerUser(req.dailyCapPerUser());
        if (req.programId() != null && tag.getKind() == NfcTag.Kind.STAMP) {
            tag.setProgram(programOf(tag.getCompany(), req.programId()));
        }
        return NfcTagResponse.of(tag, false);
    }

    /** The owner reads a card with their phone to see which of theirs it is. */
    @Transactional
    public NfcTagResponse identify(UUID ownerId, String token, String tagUid) {
        NfcTag tag = tags.findByTokenWithRefs(token.trim())
                .orElseThrow(() -> new AppException(ErrorCode.NFC_UNKNOWN_TAG));
        Company mine = companies.findByOwnerId(ownerId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        if (!tag.getCompany().getId().equals(mine.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        String uid = tagUid == null ? "" : tagUid.trim().toUpperCase();
        if (!uid.isEmpty()) {
            if (tag.getTagUid() == null || tag.getTagUid().isBlank()) {
                tag.setTagUid(uid);
            } else if (!tag.getTagUid().equalsIgnoreCase(uid)) {
                throw new AppException(ErrorCode.NFC_UNKNOWN_TAG);
            }
        }
        return NfcTagResponse.of(tag, false);
    }

    /** Sets what the card does. Targets must belong to the card's own company. */
    @Transactional
    public NfcTagResponse assign(UUID tagId, NfcAssignRequest req) {
        NfcTag tag = get(tagId);
        NfcTag.Kind kind = parseKind(req.kind());
        tag.setProgram(kind == NfcTag.Kind.STAMP
                ? programOf(tag.getCompany(), req.programId()) : null);
        tag.setKind(kind);
        if (req.pointsValue() != null) tag.setPointsValue(req.pointsValue());
        if (req.stampsValue() != null) tag.setStampsValue(req.stampsValue());
        if (req.label() != null) tag.setLabel(req.label());
        if (req.cooldownSec() != null) tag.setCooldownSec(req.cooldownSec());
        if (req.dailyCapPerUser() != null) tag.setDailyCapPerUser(req.dailyCapPerUser());
        return NfcTagResponse.of(tag, false);
    }

    private LoyaltyProgram programOf(Company company, UUID programId) {
        if (programId == null) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        LoyaltyProgram program = programs.findById(programId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        if (!program.getCompany().getId().equals(company.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        // A card bound to a paused or ended campaign would refuse every tap.
        if (!program.isOpen()) {
            throw new AppException(ErrorCode.PROGRAM_CLOSED);
        }
        return program;
    }


    private static NfcTag.Kind parseKind(String value) {
        try {
            return NfcTag.Kind.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
    }

    private static NfcTag.Status parseStatus(String value) {
        try {
            return NfcTag.Status.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public NfcTag get(UUID tagId) {
        return tags.findByIdWithRefs(tagId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
    }

    /** Company that owns a card — used for the owner-scope check. */
    @Transactional(readOnly = true)
    public UUID companyIdOf(UUID tagId) {
        return get(tagId).getCompany().getId();
    }

    @Transactional(readOnly = true)
    public List<NfcTagResponse> forCompany(UUID companyId) {
        return tags.findAllForCompany(companyId).stream()
                .map(t -> NfcTagResponse.of(t, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<NfcTagResponse> all() {
        return tags.findAllWithRefs().stream()
                .map(t -> NfcTagResponse.of(t, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<NfcTap> companyTaps(UUID companyId, int days, int limit) {
        return taps.forCompanySince(companyId,
                Instant.now().minus(Math.max(1, days), ChronoUnit.DAYS),
                PageRequest.of(0, Math.min(Math.max(limit, 1), 500)));
    }

    @Transactional(readOnly = true)
    public List<NfcTap> latestTaps(boolean suspiciousOnly, int limit) {
        return taps.latest(suspiciousOnly,
                PageRequest.of(0, Math.min(Math.max(limit, 1), 500)));
    }

    private static String newToken() {
        byte[] buf = new byte[24];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
