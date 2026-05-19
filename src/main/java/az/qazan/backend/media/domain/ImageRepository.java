package az.qazan.backend.media.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImageRepository extends JpaRepository<StoredImage, UUID> {
}
