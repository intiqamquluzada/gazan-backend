package az.qazan.backend.coins.api.dto;

import java.util.List;

/**
 * The customer's whole coin picture in one payload: total spendable
 * balance, the per-business breakdown, and the most recent ledger
 * entries for an activity feed.
 */
public record CoinSummaryResponse(
        long total,
        List<CompanyBalanceResponse> companies,
        List<CoinTxnResponse> recent
) {
}
