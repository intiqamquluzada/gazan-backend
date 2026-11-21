package az.qazan.backend.nfc.application;

import az.qazan.backend.nfc.domain.NfcTap;
import az.qazan.backend.nfc.domain.NfcTapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Writes the audit row for a REJECTED tap in its own transaction.
 *
 * <p>A rejection ends with an exception, which rolls the tap transaction
 * back — including the audit row, if it were written there. Without this
 * separate bean the abuse attempts would leave no trace at all, which is
 * exactly the data the fraud screen needs.
 */
@Service
@RequiredArgsConstructor
public class NfcAuditService {

    private final NfcTapRepository taps;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRejected(UUID tagId, UUID userId, UUID companyId,
                               NfcTap.Result result, Integer distanceM, String deviceId,
                               String clientTapId, boolean suspicious, String note) {
        taps.save(NfcTap.builder()
                .tagId(tagId)
                .userId(userId)
                .companyId(companyId)
                // Always 0 here: uq_nfc_taps_counter is for accepted reads only.
                .counter(0L)
                .result(result)
                .pointsDelta(0)
                .stampsDelta(0)
                .distanceM(distanceM)
                .deviceId(deviceId)
                // Deliberately NOT stored as clientTapId: a rejected attempt must
                // not make a later retry of the same id look "already processed".
                .clientTapId(null)
                .suspicious(suspicious)
                .note(trim(note == null ? clientTapId : note + " / " + clientTapId))
                .build());
    }

    private static String trim(String s) {
        if (s == null) return null;
        return s.length() > 255 ? s.substring(0, 255) : s;
    }
}
