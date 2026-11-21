package az.qazan.backend.admin.domain;

import java.util.UUID;

public interface CompanyCoinTotals {
    UUID getCompanyId();
    String getName();
    long getEarned();
    long getSpent();
    long getGrants();
}
