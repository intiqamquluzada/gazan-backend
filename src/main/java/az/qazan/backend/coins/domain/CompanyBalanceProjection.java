package az.qazan.backend.coins.domain;

import java.util.UUID;

/** Read projection: a customer's net coin balance at one company. */
public interface CompanyBalanceProjection {
    UUID getCompanyId();

    String getCompanyName();

    long getBalance();
}
