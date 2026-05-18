package az.qazan.backend.coins.application;

import az.qazan.backend.coins.api.dto.CoinSummaryResponse;
import az.qazan.backend.coins.api.dto.CoinTxnResponse;
import az.qazan.backend.coins.api.dto.CompanyBalanceResponse;
import az.qazan.backend.coins.domain.CoinTransaction;
import az.qazan.backend.coins.domain.CoinTransactionRepository;
import az.qazan.backend.coins.domain.CoinTransactionType;
import az.qazan.backend.common.exception.BadRequestException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoinService {

    private final CoinTransactionRepository ledger;
    private final UserRepository users;
    private final CompanyRepository companies;

    @Transactional(readOnly = true)
    public CoinSummaryResponse summary(UUID userId) {
        long total = ledger.balanceOf(userId);
        List<CompanyBalanceResponse> perCompany = ledger.balancesByCompany(userId)
                .stream()
                .map(p -> new CompanyBalanceResponse(
                        p.getCompanyId(), p.getCompanyName(), p.getBalance()))
                .toList();
        List<CoinTxnResponse> recent = ledger
                .findTop15ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(CoinService::toTxn)
                .toList();
        return new CoinSummaryResponse(total, perCompany, recent);
    }

    /** Credit coins to a user (scan reward / seed / future earning hook). */
    @Transactional
    public CoinSummaryResponse grant(UUID userId, UUID companyId,
                                     int amount, String note) {
        if (amount <= 0) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        record(userId, companyId, amount, CoinTransactionType.EARN, note);
        return summary(userId);
    }

    /** Spend coins on a reward / cash discount. */
    @Transactional
    public CoinSummaryResponse spend(UUID userId, UUID companyId,
                                     int amount, String note) {
        if (amount <= 0) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        if (ledger.balanceOf(userId) < amount) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        record(userId, companyId, -amount, CoinTransactionType.SPEND, note);
        return summary(userId);
    }

    private void record(UUID userId, UUID companyId, int signedAmount,
                         CoinTransactionType type, String note) {
        User user = users.findById(userId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
        Company company = companyId == null ? null : companies.findById(companyId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        ledger.save(CoinTransaction.builder()
                .user(user)
                .company(company)
                .amount(signedAmount)
                .type(type)
                .note(note)
                .build());
    }

    private static CoinTxnResponse toTxn(CoinTransaction t) {
        Company c = t.getCompany();
        return new CoinTxnResponse(
                t.getId(),
                c == null ? null : c.getId(),
                c == null ? null : c.getName(),
                t.getAmount(),
                t.getType().name(),
                t.getNote(),
                t.getCreatedAt()
        );
    }
}
