package az.qazan.backend.media.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImageRepository extends JpaRepository<StoredImage, UUID> {

    /** Ids only, so big blobs aren't all loaded at once. */
    @org.springframework.data.jpa.repository.Query(
            "select i.id from StoredImage i where i.sizeBytes > :bytes")
    java.util.List<UUID> idsLargerThan(@org.springframework.data.repository.query.Param("bytes") int bytes);

}
