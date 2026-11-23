package az.qazan.backend.media.application;

import az.qazan.backend.media.domain.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

/**
 * On start, shrinks images that were stored before uploads were optimised.
 * Idempotent: an image is only rewritten when the result is smaller, and
 * shrunk images fall under the threshold, so later starts find nothing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LargeImageShrinker implements ApplicationRunner {

    private static final int THRESHOLD_BYTES = 600 * 1024;

    private final ImageRepository images;
    private final TransactionTemplate tx;

    @Override
    public void run(ApplicationArguments args) {
        List<UUID> ids;
        try {
            ids = images.idsLargerThan(THRESHOLD_BYTES);
        } catch (RuntimeException e) {
            log.warn("Image shrink skipped: {}", e.getMessage());
            return;
        }
        for (UUID id : ids) {
            try {
                tx.executeWithoutResult(s -> images.findById(id).ifPresent(img -> {
                    ImageOptimizer.Result r = ImageOptimizer.optimize(img.getBytes(), img.getContentType());
                    if (r.bytes().length < img.getSizeBytes()) {
                        log.info("Shrunk image {}: {} KB -> {} KB", id,
                                img.getSizeBytes() / 1024, r.bytes().length / 1024);
                        img.setBytes(r.bytes());
                        img.setContentType(r.contentType());
                        img.setSizeBytes(r.bytes().length);
                    }
                }));
            } catch (RuntimeException e) {
                log.warn("Could not shrink image {}: {}", id, e.getMessage());
            }
        }
    }
}
