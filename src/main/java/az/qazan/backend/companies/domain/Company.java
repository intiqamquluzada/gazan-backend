package az.qazan.backend.companies.domain;

import az.qazan.backend.common.audit.BaseEntity;
import az.qazan.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "companies",
        indexes = {
                @Index(name = "idx_companies_category", columnList = "category"),
                @Index(name = "idx_companies_owner", columnList = "owner_id"),
                @Index(name = "idx_companies_featured", columnList = "is_featured"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "tagline", length = 240)
    private String tagline;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private BusinessCategory category;

    /** Emoji used as a graceful fallback when no logo image is uploaded. */
    @Column(name = "logo_emoji", length = 8)
    private String logoEmoji;

    /** Brand color stored as the unsigned 32-bit ARGB value (e.g. 0xFF7B3F00). */
    @Column(name = "cover_color_hex", nullable = false)
    private long coverColorHex;

    @Column(name = "address", length = 240)
    private String address;

    @Column(name = "rating", precision = 2)
    private Double rating;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "is_featured", nullable = false)
    private boolean featured;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    // ── Editable profile (managed by the owner) ────────────────────────

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "instagram", length = 64)
    private String instagram;

    @Column(name = "working_hours", length = 120)
    private String workingHours;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    /** CSV of amenity codes, e.g. WIFI,WORKSPACE,MEETING,GARDEN,PARKING,VEGAN,PET */
    @Column(name = "amenities", length = 255)
    private String amenities;

    /** Newline-separated image URLs for the profile photo carousel. */
    @Column(name = "photo_urls", length = 2000)
    private String photoUrls;

    @Column(name = "menu_url", length = 512)
    private String menuUrl;
}
