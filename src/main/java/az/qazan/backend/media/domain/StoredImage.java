package az.qazan.backend.media.domain;

import az.qazan.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An uploaded image kept in Postgres ({@code bytea}). DB storage is a
 * deliberate trade-off: the prod container filesystem is ephemeral and
 * there is no object store, so DB-backed blobs are the only thing that
 * survives the frequent image rebuilds. Sizes are capped on upload.
 */
@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredImage extends BaseEntity {

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "bytes", nullable = false)
    private byte[] bytes;
}
