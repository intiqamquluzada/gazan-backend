package az.qazan.backend.push.application;

import az.qazan.backend.push.domain.DevicePlatform;
import az.qazan.backend.push.domain.DeviceToken;
import az.qazan.backend.push.domain.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Owns device-token lifecycle and fan-out of push notifications. The
 * notifications module calls {@link #pushToUser}/{@link #pushToAll} right
 * after it writes a row to the in-app inbox, so the two stay in sync.
 */
@Service
@RequiredArgsConstructor
public class PushService {

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
        List<String> targets = tokens.findByUserId(userId).stream()
                .map(DeviceToken::getToken)
                .toList();
        sender.send(targets, title, body);
    }

    @Transactional(readOnly = true)
    public void pushToAll(String title, String body) {
        List<String> targets = tokens.findAll().stream()
                .map(DeviceToken::getToken)
                .toList();
        sender.send(targets, title, body);
    }
}
