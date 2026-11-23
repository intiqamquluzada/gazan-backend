package az.qazan.backend.push.application;

import az.qazan.backend.push.domain.DevicePlatform;
import az.qazan.backend.push.domain.DeviceToken;
import az.qazan.backend.push.domain.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns device-token lifecycle and fan-out of push notifications. The
 * notifications module calls these right after it writes a row to the
 * in-app inbox, so the two stay in sync; delivery itself happens after
 * commit (see {@link PushSender}).
 */
@Service
@RequiredArgsConstructor
public class PushService {

    /** Opens the in-app inbox when the notification is tapped. */
    public static final Map<String, String> OPEN_INBOX = Map.of("route", "/notifications");

    private final DeviceTokenRepository tokens;
    private final PushSender sender;

    /** Upsert: re-point an existing token to its latest owner/platform. */
    @Transactional
    public void register(UUID userId, String token, DevicePlatform platform) {
        DeviceToken dt = tokens.findByToken(token).orElseGet(DeviceToken::new);
        dt.setUserId(userId);
        dt.setToken(token);
        dt.setPlatform(platform);
        tokens.save(dt);
    }

    /** Called on sign-out so the device stops receiving this account's pushes. */
    @Transactional
    public void unregister(String token) {
        tokens.deleteByToken(token);
    }

    @Transactional(readOnly = true)
    public void pushToUser(UUID userId, String title, String body) {
        pushToUsers(List.of(userId), title, body, OPEN_INBOX);
    }

    /** Like {@link #pushToUsers}, but skips users who turned the category off. */
    @Transactional(readOnly = true)
    public void pushToUsers(Collection<UUID> userIds, String title, String body,
                            Map<String, String> data, PushCategory category) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<String> targets = switch (category) {
            case REWARDS -> tokens.findTokensWantingRewards(userIds);
            case ALWAYS -> tokenValues(tokens.findByUserIdIn(userIds));
            // Offers and marketing never go to a hand-picked list of users.
            case OFFERS, MARKETING -> throw new IllegalArgumentException(category.name());
        };
        sender.send(targets, title, body, data);
    }

    @Transactional(readOnly = true)
    public void pushToUsers(Collection<UUID> userIds, String title, String body,
                            Map<String, String> data) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        sender.send(tokenValues(tokens.findByUserIdIn(userIds)), title, body, data);
    }

    /** Everyone holding a loyalty card at the venue. */
    @Transactional(readOnly = true)
    public void pushToCardholders(UUID companyId, String title, String body) {
        sender.send(tokens.findTokensOfCardholdersWantingOffers(companyId), title, body, OPEN_INBOX);
    }

    @Transactional(readOnly = true)
    public void pushToAll(String title, String body) {
        sender.send(tokens.findTokensWantingMarketing(), title, body, OPEN_INBOX);
    }

    private static List<String> tokenValues(List<DeviceToken> list) {
        return list.stream().map(DeviceToken::getToken).distinct().toList();
    }
}
