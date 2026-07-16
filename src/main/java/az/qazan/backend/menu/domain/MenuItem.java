package az.qazan.backend.menu.domain;

import az.qazan.backend.common.audit.BaseEntity;
import az.qazan.backend.companies.domain.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;

/** One dish/product line on a business's structured menu. */
@Entity
@Table(
        name = "menu_items",
        indexes = {
                @Index(name = "idx_menu_items_company", columnList = "company_id, sort_order"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "category", nullable = false, length = 80)
    private String category;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", length = 400)
    private String description;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
