package az.qazan.backend.media.application;

import az.qazan.backend.common.exception.BadRequestException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.media.domain.ImageRepository;
import az.qazan.backend.media.domain.StoredImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    /** Hard cap independent of the multipart limit (defence in depth). */
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private final ImageRepository images;

    @Transactional
    public StoredImage store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        String type = file.getContentType();
        if (type == null || !ALLOWED.contains(type.toLowerCase())) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        return images.save(StoredImage.builder()
                .contentType(type.toLowerCase())
                .sizeBytes(data.length)
                .bytes(data)
                .build());
    }

    @Transactional(readOnly = true)
    public StoredImage get(UUID id) {
        return images.findById(id)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
    }
}
