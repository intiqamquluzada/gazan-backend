package az.qazan.backend.companies.domain;

/** New venues wait for an admin; only APPROVED ones reach customers. */
public enum CompanyStatus {
    PENDING,
    APPROVED,
    SUSPENDED
}
