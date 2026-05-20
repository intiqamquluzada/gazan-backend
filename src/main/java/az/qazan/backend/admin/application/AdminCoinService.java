package az.qazan.backend.admin.application;

import az.qazan.backend.admin.api.dto.AdminCoinTxnResponse;
import az.qazan.backend.admin.api.dto.PageResponse;
import az.qazan.backend.admin.domain.AdminCoinRepository;
import az.qazan.backend.admin.domain.AdminCompanyRepository;
import az.qazan.backend.admin.domain.AdminUserRepository;
import az.qazan.backend.coins.domain.CoinTransaction;
import az.qazan.backend.coins.domain.CoinTransactionType;
import az.qazan.backend.common.exception.BadRequestException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCoinService {

    private final AdminCoinRepository coins;
    private final AdminUserRepository users;
    private final AdminCompanyRepository companies;

    @Transactional(readOnly = true)
    public PageResponse<AdminCoinTxnResponse> transactions(int page, int size) {
        Page<CoinTransaction> result = coins.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, Math.min(size, 100)));
        return PageResponse.of(result, AdminCoinTxnResponse::from);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> summary() {
        return Map.of(
                "circulating", coins.circulating(),
                "earned", coins.totalEarned(),
                "spent", coins.totalSpent(),
                "transactions", coins.count()
        );
    }

    /** Manual signed correction to a user's balance (audited as a ledger row). */
    @Transactional
    public AdminCoinTxnResponse adjust(UUID userId, UUID companyId,
                                       int amount, String note) {
        if (amount == 0) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        User user = users.findById(userId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
        Company company = companyId == null ? null : companies.findById(companyId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));

        String trimmed = note == null || note.isBlank()
                ? "Admin düzəlişi" : note.trim();
        CoinTransaction saved = coins.save(CoinTransaction.builder()
                .user(user)
                .company(company)
                .amount(amount)
                .type(amount > 0 ? CoinTransactionType.EARN : CoinTransactionType.SPEND)
                .note("Admin: " + trimmed)
                .build());
        return AdminCoinTxnResponse.from(saved);
    }
}
