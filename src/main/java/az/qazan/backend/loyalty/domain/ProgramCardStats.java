package az.qazan.backend.loyalty.domain;

import java.util.UUID;

public interface ProgramCardStats {
    UUID getProgramId();
    long getMembers();
    long getCompleted();
    long getRedeemed();
}
